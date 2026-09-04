package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.dto.ChatMessage;
import com.buffturf.buffturf_backend.dto.ChatRequest;
import com.buffturf.buffturf_backend.dto.ChatResponse;
import com.buffturf.buffturf_backend.dto.SlotReservationResponse;
import com.buffturf.buffturf_backend.dto.SlotResponseDto;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.repository.SlotRepository;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final TurfRepository turfRepository;
    private final SlotService slotService;
    private final SlotLockService slotLockService;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public ChatService(TurfRepository turfRepository,
                       SlotService slotService,
                       SlotLockService slotLockService) {
        this.turfRepository = turfRepository;
        this.slotService = slotService;
        this.slotLockService = slotLockService;
        this.restTemplate = new RestTemplate();
    }

    public ChatResponse handleChat(ChatRequest chatRequest) {
        return handleChat(chatRequest, null);
    }

    public ChatResponse handleChat(ChatRequest chatRequest, String currentUserEmail) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equalsIgnoreCase("your_actual_api_key_here")) {
            return new ChatResponse("AI Assistant is currently offline (Gemini API Key is not configured). Please contact the administrator or set GEMINI_API_KEY in the backend environment variables.");
        }

        try {
            String url = geminiUrl + "?key=" + geminiApiKey;

            List<Map<String, Object>> contents = new ArrayList<>();

            if (chatRequest.getMessages() != null) {
                for (ChatMessage msg : chatRequest.getMessages()) {
                    Map<String, Object> contentMap = new HashMap<>();
                    String role = msg.getRole().equalsIgnoreCase("user") ? "user" : "model";
                    contentMap.put("role", role);

                    List<Map<String, Object>> parts = new ArrayList<>();
                    Map<String, Object> part = new HashMap<>();
                    part.put("text", msg.getContent());
                    parts.add(part);
                    contentMap.put("parts", parts);

                    contents.add(contentMap);
                }
            }

            // System Instruction
            Map<String, Object> systemInstruction = new HashMap<>();
            List<Map<String, Object>> sysParts = new ArrayList<>();
            Map<String, Object> sysPart = new HashMap<>();
            sysPart.put("text", buildSystemPrompt());
            sysParts.add(sysPart);
            systemInstruction.put("parts", sysParts);

            // Tools (Function Declarations)
            List<Map<String, Object>> tools = buildTools();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Agent Loop: Allows Gemini to call tools and receive responses (up to 3 turns)
            int maxTurns = 3;
            while (maxTurns-- > 0) {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("contents", contents);
                requestBody.put("systemInstruction", systemInstruction);
                requestBody.put("tools", tools);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
                Map<String, Object> body = responseEntity.getBody();

                Map<String, Object> candidate = getFirstCandidate(body);
                if (candidate == null) {
                    return new ChatResponse("I couldn't process a response right now. Please try again.");
                }

                Map<String, Object> candidateContent = (Map<String, Object>) candidate.get("content");
                if (candidateContent == null || !candidateContent.containsKey("parts")) {
                    return new ChatResponse("I couldn't process a response right now. Please try again.");
                }

                List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                Map<String, Object> functionCall = findFunctionCall(parts);

                if (functionCall != null) {
                    // Append model's tool call turn to contents
                    contents.add(candidateContent);

                    String functionName = (String) functionCall.get("name");
                    Map<String, Object> args = (Map<String, Object>) functionCall.get("args");

                    // Execute tool internally
                    Map<String, Object> functionResult = executeTool(functionName, args, currentUserEmail);

                    // Build function response turn
                    Map<String, Object> funcRespContent = new HashMap<>();
                    funcRespContent.put("role", "function");

                    Map<String, Object> funcRespPart = new HashMap<>();
                    Map<String, Object> funcRespWrapper = new HashMap<>();
                    funcRespWrapper.put("name", functionName);
                    funcRespWrapper.put("response", Map.of("name", functionName, "content", functionResult));
                    funcRespPart.put("functionResponse", funcRespWrapper);

                    funcRespContent.put("parts", List.of(funcRespPart));
                    contents.add(funcRespContent);

                    // Continue agent loop to let Gemini formulate the final answer or call another tool
                    continue;
                }

                // If no function call, extract and return final text response
                String aiText = extractText(parts);
                if (aiText.isEmpty()) {
                    aiText = "I processed your request, but could not produce a text summary. Please try again.";
                }
                return new ChatResponse(aiText);
            }

            return new ChatResponse("I processed your query, but reached the maximum tool reasoning steps. Please try rephrasing.");

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            e.printStackTrace();
            int statusCode = e.getStatusCode().value();
            if (statusCode == 429) {
                return new ChatResponse("The AI assistant is temporarily busy due to high demand. Please wait a moment and try again.");
            } else if (statusCode == 404) {
                return new ChatResponse("AI model configuration error. Please contact the administrator.");
            } else {
                return new ChatResponse("I apologize, but I encountered an error trying to process your request. Please try again later.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse("I apologize, but I encountered an error trying to process your request. Please try again later.");
        }
    }

    // -------------------------------------------------------------------------
    // Tool Execution
    // -------------------------------------------------------------------------

    private Map<String, Object> executeTool(String functionName, Map<String, Object> args, String currentUserEmail) {
        if (args == null) args = Collections.emptyMap();

        try {
            switch (functionName) {
                case "search_turfs":
                    return searchTurfs(args);

                case "get_turf_details":
                    return getTurfDetails(args);

                case "check_slot_availability":
                    return checkSlotAvailability(args, currentUserEmail);

                case "hold_slot_reservation":
                    return holdSlotReservation(args, currentUserEmail);

                default:
                    return Map.of("error", "Unknown tool: " + functionName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Execution failed: " + e.getMessage());
        }
    }

    private Map<String, Object> searchTurfs(Map<String, Object> args) {
        String sport = args.get("sport") != null ? args.get("sport").toString().trim().toLowerCase() : null;
        String location = args.get("location") != null ? args.get("location").toString().trim().toLowerCase() : null;
        Double maxPrice = null;
        if (args.get("max_price") != null) {
            try {
                maxPrice = Double.parseDouble(args.get("max_price").toString());
            } catch (Exception ignored) {}
        }

        List<Turf> all = turfRepository.findAll();
        Double finalMaxPrice = maxPrice;

        List<Map<String, Object>> matches = all.stream()
                .filter(t -> sport == null || (t.getSportType() != null && t.getSportType().toLowerCase().contains(sport)))
                .filter(t -> location == null || (t.getLocation() != null && t.getLocation().toLowerCase().contains(location))
                        || (t.getAddress() != null && t.getAddress().toLowerCase().contains(location)))
                .filter(t -> finalMaxPrice == null || (t.getPricePerHour() != null && t.getPricePerHour() <= finalMaxPrice))
                .limit(8)
                .map(t -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", t.getId());
                    map.put("name", t.getName());
                    map.put("sportType", t.getSportType());
                    map.put("location", t.getLocation());
                    map.put("address", t.getAddress());
                    map.put("pricePerHour", t.getPricePerHour());
                    map.put("openHours", t.getOpenTime() + " - " + t.getCloseTime());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_found", matches.size());
        result.put("turfs", matches);
        return result;
    }

    private Map<String, Object> getTurfDetails(Map<String, Object> args) {
        String turfName = (String) args.get("turf_name");
        Turf turf = findTurf(turfName);
        if (turf == null) {
            return Map.of("error", "No turf found matching name: " + turfName);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("id", turf.getId());
        details.put("name", turf.getName());
        details.put("sportType", turf.getSportType());
        details.put("location", turf.getLocation());
        details.put("address", turf.getAddress());
        details.put("pricePerHour", turf.getPricePerHour());
        details.put("openTime", turf.getOpenTime());
        details.put("closeTime", turf.getCloseTime());
        details.put("description", turf.getDescription());
        return details;
    }

    private Map<String, Object> checkSlotAvailability(Map<String, Object> args, String currentUserEmail) {
        String turfName = (String) args.get("turf_name");
        String dateStr = (String) args.get("date");

        Turf turf = findTurf(turfName);
        if (turf == null) {
            return Map.of("error", "No turf found with name: " + turfName);
        }

        LocalDate date = parseLocalDate(dateStr);
        List<SlotResponseDto> enrichedSlots = slotService.getEnrichedSlotsByTurfAndDate(turf.getId(), date, currentUserEmail);

        List<Map<String, Object>> availableSlots = new ArrayList<>();
        int bookedCount = 0;
        int heldCount = 0;

        for (SlotResponseDto slot : enrichedSlots) {
            if (Boolean.TRUE.equals(slot.getIsAvailable()) && "AVAILABLE".equalsIgnoreCase(slot.getLockStatus())) {
                Map<String, Object> sMap = new LinkedHashMap<>();
                sMap.put("slotId", slot.getId());
                sMap.put("time", slot.getStartTime() + " - " + slot.getEndTime());
                sMap.put("pricePerHour", turf.getPricePerHour());
                availableSlots.add(sMap);
            } else if ("BOOKED".equalsIgnoreCase(slot.getLockStatus())) {
                bookedCount++;
            } else {
                heldCount++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("turf_id", turf.getId());
        result.put("turf_name", turf.getName());
        result.put("date", date.toString());
        result.put("price_per_hour", turf.getPricePerHour());
        result.put("available_slots_count", availableSlots.size());
        result.put("available_slots", availableSlots);
        result.put("booked_slots_count", bookedCount);
        result.put("currently_locked_slots_count", heldCount);
        return result;
    }

    private Map<String, Object> holdSlotReservation(Map<String, Object> args, String currentUserEmail) {
        if (currentUserEmail == null || currentUserEmail.trim().isEmpty()) {
            return Map.of(
                    "success", false,
                    "error", "User is not logged in. Please tell the user to log into their BuffTurf account before placing a slot hold."
            );
        }

        String turfName = (String) args.get("turf_name");
        String dateStr = (String) args.get("date");
        String startTimeStr = (String) args.get("start_time");

        Turf turf = findTurf(turfName);
        if (turf == null) {
            return Map.of("success", false, "error", "No turf found with name: " + turfName);
        }

        LocalDate date = parseLocalDate(dateStr);
        LocalTime startTime = parseLocalTime(startTimeStr);
        if (startTime == null) {
            return Map.of("success", false, "error", "Invalid start time format: " + startTimeStr + ". Please specify HH:mm.");
        }

        List<Slot> slots = slotService.getSlotsByTurfAndDate(turf.getId(), date);
        Slot targetSlot = slots.stream()
                .filter(s -> s.getStartTime() != null && s.getStartTime().getHour() == startTime.getHour()
                        && s.getStartTime().getMinute() == startTime.getMinute())
                .findFirst()
                .orElse(null);

        if (targetSlot == null) {
            return Map.of("success", false, "error", "No slot found at " + startTimeStr + " for " + turf.getName() + " on " + date);
        }

        SlotReservationResponse holdRes = slotLockService.acquireHold(List.of(targetSlot.getId()), currentUserEmail, 600L);
        if (holdRes != null && holdRes.isSuccess()) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("slot_id", targetSlot.getId());
            res.put("turf_name", turf.getName());
            res.put("turf_id", turf.getId());
            res.put("date", date.toString());
            res.put("time", targetSlot.getStartTime() + " - " + targetSlot.getEndTime());
            res.put("hold_duration_minutes", 10);
            res.put("booking_url", "/turfs/" + turf.getId());
            res.put("message", "Slot held successfully for 10 minutes!");
            return res;
        } else {
            String reason = (holdRes != null && holdRes.getMessage() != null) ? holdRes.getMessage() : "Slot could not be locked. It is already booked or held by someone else.";
            return Map.of("success", false, "error", reason);
        }
    }

    // -------------------------------------------------------------------------
    // Gemini Tool Definitions
    // -------------------------------------------------------------------------

    private List<Map<String, Object>> buildTools() {
        List<Map<String, Object>> declarations = new ArrayList<>();

        // 1. search_turfs
        Map<String, Object> searchTurfsDecl = new LinkedHashMap<>();
        searchTurfsDecl.put("name", "search_turfs");
        searchTurfsDecl.put("description", "Search and filter sports venues by sport type (e.g. Football, Cricket, Badminton, Tennis), location or city, and/or maximum budget per hour.");
        Map<String, Object> searchProps = new LinkedHashMap<>();
        searchProps.put("sport", Map.of("type", "STRING", "description", "Sport type: Football, Cricket, Badminton, Tennis"));
        searchProps.put("location", Map.of("type", "STRING", "description", "Locality or city name, e.g. Kothrud, Baner, Pune"));
        searchProps.put("max_price", Map.of("type", "NUMBER", "description", "Maximum hourly rate in INR"));
        searchTurfsDecl.put("parameters", Map.of("type", "OBJECT", "properties", searchProps));
        declarations.add(searchTurfsDecl);

        // 2. get_turf_details
        Map<String, Object> getDetailsDecl = new LinkedHashMap<>();
        getDetailsDecl.put("name", "get_turf_details");
        getDetailsDecl.put("description", "Get detailed information about a specific turf including full address, amenities, operating hours, and pricing.");
        Map<String, Object> getDetailsProps = new LinkedHashMap<>();
        getDetailsProps.put("turf_name", Map.of("type", "STRING", "description", "The name or partial name of the turf"));
        getDetailsDecl.put("parameters", Map.of("type", "OBJECT", "properties", getDetailsProps, "required", List.of("turf_name")));
        declarations.add(getDetailsDecl);

        // 3. check_slot_availability
        Map<String, Object> checkSlotsDecl = new LinkedHashMap<>();
        checkSlotsDecl.put("name", "check_slot_availability");
        checkSlotsDecl.put("description", "Check real-time live availability of booking slots for a specific sports turf on a given date. Returns open time windows and pricing.");
        Map<String, Object> checkSlotsProps = new LinkedHashMap<>();
        checkSlotsProps.put("turf_name", Map.of("type", "STRING", "description", "Name or partial name of the sports turf"));
        checkSlotsProps.put("date", Map.of("type", "STRING", "description", "Date in YYYY-MM-DD format. If user says 'today' or 'tomorrow', compute the appropriate YYYY-MM-DD date based on today's date."));
        checkSlotsDecl.put("parameters", Map.of("type", "OBJECT", "properties", checkSlotsProps, "required", List.of("turf_name", "date")));
        declarations.add(checkSlotsDecl);

        // 4. hold_slot_reservation
        Map<String, Object> holdSlotDecl = new LinkedHashMap<>();
        holdSlotDecl.put("name", "hold_slot_reservation");
        holdSlotDecl.put("description", "Place an atomic 10-minute temporary hold/lock on a specific time slot for the user so they can complete checkout. Requires the user to be logged in.");
        Map<String, Object> holdProps = new LinkedHashMap<>();
        holdProps.put("turf_name", Map.of("type", "STRING", "description", "Name of the turf"));
        holdProps.put("date", Map.of("type", "STRING", "description", "Date in YYYY-MM-DD format"));
        holdProps.put("start_time", Map.of("type", "STRING", "description", "Slot start time in HH:mm format, e.g. '18:00' or '20:00'"));
        holdSlotDecl.put("parameters", Map.of("type", "OBJECT", "properties", holdProps, "required", List.of("turf_name", "date", "start_time")));
        declarations.add(holdSlotDecl);

        Map<String, Object> toolMap = new HashMap<>();
        toolMap.put("functionDeclarations", declarations);
        return List.of(toolMap);
    }

    // -------------------------------------------------------------------------
    // System Instruction
    // -------------------------------------------------------------------------

    private String buildSystemPrompt() {
        LocalDate today = LocalDate.now(IST_ZONE);
        StringBuilder sb = new StringBuilder();
        sb.append("You are the intelligent AI Sports Concierge for BuffTurf, an elite sports turf and arena reservation platform.\n");
        sb.append("Today's Date is: ").append(today.toString()).append(" (Asia/Kolkata timezone).\n\n");
        sb.append("Capabilities and Guidelines:\n");
        sb.append("1. ALWAYS use your tools:\n");
        sb.append("   - Use 'search_turfs' when the user asks for venues by sport, location, or budget.\n");
        sb.append("   - Use 'check_slot_availability' whenever the user asks about available times, open slots, or dates for a turf. NEVER guess or invent slot availability!\n");
        sb.append("   - Use 'hold_slot_reservation' when a user wants to reserve, lock, or hold a slot.\n");
        sb.append("   - Use 'get_turf_details' when asked for specific address, description, or operating hours.\n");
        sb.append("2. Always present prices in Indian Rupees (₹), e.g. ₹800/hr.\n");
        sb.append("3. Format turf links in Markdown using their Database ID: [Turf Name](/turfs/ID). NEVER print raw IDs alone.\n");
        sb.append("4. For booking actions or holds, guide the user to [Proceed to Turf Details](/turfs/ID).\n");
        sb.append("5. For policy questions, link to [Cancellation Policy](/cancellation-policy) or [Refund Policy](/refund-policy).\n");
        sb.append("6. Keep your answers concise, structured, friendly, and athletic.\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Helper Parsers
    // -------------------------------------------------------------------------

    private Turf findTurf(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String query = name.trim().toLowerCase();
        List<Turf> all = turfRepository.findAll();

        for (Turf t : all) {
            if (t.getName() != null && t.getName().equalsIgnoreCase(query)) return t;
        }
        for (Turf t : all) {
            if (t.getName() != null && (t.getName().toLowerCase().contains(query) || query.contains(t.getName().toLowerCase()))) {
                return t;
            }
        }
        String[] words = query.split("\\s+");
        for (Turf t : all) {
            for (String w : words) {
                if (w.length() > 3 && t.getName() != null && t.getName().toLowerCase().contains(w)) {
                    return t;
                }
            }
        }
        return null;
    }

    private LocalDate parseLocalDate(String dateStr) {
        LocalDate today = LocalDate.now(IST_ZONE);
        if (dateStr == null || dateStr.trim().isEmpty()) return today;
        String clean = dateStr.trim().toLowerCase();
        if ("today".equals(clean)) return today;
        if ("tomorrow".equals(clean)) return today.plusDays(1);
        try {
            return LocalDate.parse(clean);
        } catch (Exception e) {
            return today;
        }
    }

    private LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        String clean = timeStr.trim().toUpperCase();
        try {
            if (clean.endsWith("PM") || clean.endsWith("AM")) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                return LocalTime.parse(clean, dtf);
            }
            if (clean.length() == 5) return LocalTime.parse(clean);
            if (clean.length() == 8) return LocalTime.parse(clean);
            if (clean.matches("\\d{1,2}")) {
                return LocalTime.of(Integer.parseInt(clean), 0);
            }
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> getFirstCandidate(Map<String, Object> body) {
        if (body != null && body.containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                return candidates.get(0);
            }
        }
        return null;
    }

    private Map<String, Object> findFunctionCall(List<Map<String, Object>> parts) {
        if (parts == null) return null;
        for (Map<String, Object> part : parts) {
            if (part != null && part.containsKey("functionCall")) {
                return (Map<String, Object>) part.get("functionCall");
            }
        }
        return null;
    }

    private String extractText(List<Map<String, Object>> parts) {
        if (parts == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> part : parts) {
            if (part != null && part.containsKey("text")) {
                sb.append(part.get("text"));
            }
        }
        return sb.toString().trim();
    }
}
