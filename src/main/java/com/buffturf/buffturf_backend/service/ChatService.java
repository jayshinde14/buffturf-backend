package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.dto.ChatMessage;
import com.buffturf.buffturf_backend.dto.ChatRequest;
import com.buffturf.buffturf_backend.dto.ChatResponse;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class ChatService {

    private final TurfRepository turfRepository;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public ChatService(TurfRepository turfRepository) {
        this.turfRepository = turfRepository;
        this.restTemplate = new RestTemplate();
    }

    public ChatResponse handleChat(ChatRequest chatRequest) {
        
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equalsIgnoreCase("your_actual_api_key_here")) {
            return new ChatResponse("AI Assistant is currently offline (Gemini API Key is not configured). Please contact the administrator or set GEMINI_API_KEY in the backend environment variables.");
        }

        try {
            String url = geminiUrl + "?key=" + geminiApiKey;

            
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();

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
            requestBody.put("contents", contents);

            // Add System Instructions
            Map<String, Object> systemInstruction = new HashMap<>();
            List<Map<String, Object>> sysParts = new ArrayList<>();
            Map<String, Object> sysPart = new HashMap<>();
            sysPart.put("text", buildSystemPrompt());
            sysParts.add(sysPart);
            systemInstruction.put("parts", sysParts);
            requestBody.put("systemInstruction", systemInstruction);

            // Configure headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Call Gemini API
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = responseEntity.getBody();

            // Extract the generated text
            String aiResponseText = parseGeminiResponse(body);
            return new ChatResponse(aiResponseText);

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

    private String parseGeminiResponse(Map<String, Object> body) {
        if (body != null && body.containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                if (content != null && content.containsKey("parts")) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<String, Object> firstPart = parts.get(0);
                        return (String) firstPart.get("text");
                    }
                }
            }
        }
        return "I couldn't process a response right now. Please try again.";
    }
    
    private String buildSystemPrompt() {
        List<Turf> turfs = turfRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("You are the AI Booking Assistant for BuffTurf, a sports turf booking platform.\n");
        sb.append("Help the user find turfs, understand booking rates, sports available, and answer questions. Keep your responses friendly, concise, and helpful.\n\n");
        sb.append("Today's Date: ").append(LocalDate.now().toString()).append("\n\n");
        sb.append("Available Turfs in our Database:\n");

        if (turfs.isEmpty()) {
            sb.append("Currently, there are no turfs available for booking.\n");
        } else {
            for (Turf turf : turfs) {
                sb.append("- ").append(turf.getName()).append("\n")
                  .append("  Database ID: ").append(turf.getId()).append(" (Use ONLY for links. NEVER display or print this ID to the user)\n")
                  .append("  Location: ").append(turf.getLocation()).append("\n")
                  .append("  Address: ").append(turf.getAddress()).append("\n")
                  .append("  Sport Type: ").append(turf.getSportType()).append("\n")
                  .append("  Price: ₹").append(turf.getPricePerHour()).append(" per hour\n")
                  .append("  Open Hours: ").append(turf.getOpenTime()).append(" to ").append(turf.getCloseTime()).append("\n");
                if (turf.getDescription() != null && !turf.getDescription().trim().isEmpty()) {
                    sb.append("  Description: ").append(turf.getDescription()).append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("Important Instructions:\n");
        sb.append("1. Answer questions using the turf data above. If a user asks for a sport or location that is not available, politely say we don't have it yet.\n");
        sb.append("2. If they want to book, explain that they can click on the turf in the listing page, choose a date, and pick an available slot. Guide them to '/turfs' to browse listings.\n");
        sb.append("3. For questions about cancellations or refunds, advise them to check the Cancellation Policy or Refund Policy pages (link to '/cancellation-policy' or '/refund-policy' in markdown format).\n");
        sb.append("4. Keep formatting clean using simple Markdown list items or bold text.\n");
        sb.append("5. All prices MUST be presented in Indian Rupees (₹), e.g., ₹699 per hour. Do NOT use dollars ($).\n");
        sb.append("6. NEVER print or mention database IDs (like ID: 4, 6, etc.) to the user under any circumstances. Refer to turfs strictly by their name.\n");
        sb.append("7. If you mention a turf, link to its detail page using markdown: [Turf Name](/turfs/ID) (replace ID with the actual database ID of that turf).\n");

        return sb.toString();
    }
}
