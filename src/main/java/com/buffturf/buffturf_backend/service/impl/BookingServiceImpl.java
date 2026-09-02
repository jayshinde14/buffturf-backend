package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.BookingRequest;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.*;
import com.buffturf.buffturf_backend.repository.*;
import com.buffturf.buffturf_backend.service.BookingService;
import com.buffturf.buffturf_backend.service.EmailService;
import com.buffturf.buffturf_backend.service.QrService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buffturf.buffturf_backend.service.SlotLockService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final QrPassRepository qrPassRepository;
    private final QrService qrService;
    private final EmailService emailService;
    private final SlotLockService slotLockService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              TurfRepository turfRepository,
                              SlotRepository slotRepository,
                              UserRepository userRepository,
                              QrPassRepository qrPassRepository,
                              QrService qrService,
                              EmailService emailService,
                              SlotLockService slotLockService) {
        this.bookingRepository = bookingRepository;
        this.turfRepository = turfRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.qrPassRepository = qrPassRepository;
        this.qrService = qrService;
        this.emailService = emailService;
        this.slotLockService = slotLockService;
    }

    @Override
    @Transactional
    public Booking createBooking(BookingRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (user.isBanned()) {
            throw new ApiException("You are banned and cannot make bookings.", HttpStatus.FORBIDDEN);
        }

        Turf turf = turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> new ResourceNotFoundException("Turf not found: " + request.getTurfId()));

        List<Slot> slots = slotRepository.findAllById(request.getSlotIds());
        if (slots.isEmpty() || slots.size() != request.getSlotIds().size()) {
            throw new ApiException("One or more slots not found", HttpStatus.BAD_REQUEST);
        }

        slots.sort((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()));
        for (int i = 0; i < slots.size() - 1; i++) {
            if (!slots.get(i).getEndTime().equals(slots.get(i + 1).getStartTime())) {
                throw new ApiException("Selected slots must be continuous (back-to-back).", HttpStatus.BAD_REQUEST);
            }
        }

        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        LocalDateTime now = LocalDateTime.now(istZone);
        for (Slot slot : slots) {
            if (!slot.getIsAvailable()) {
                throw new ApiException("One or more slots are already booked!", HttpStatus.BAD_REQUEST);
            }
            String lockOwner = slotLockService.getLockOwner(slot.getId());
            if (lockOwner != null && !lockOwner.equalsIgnoreCase(email)) {
                throw new ApiException("Slot #" + slot.getId() + " is currently in checkout by another customer.", HttpStatus.CONFLICT);
            }
            LocalDateTime slotDateTime = LocalDateTime.of(request.getBookingDate(), slot.getStartTime());
            if (slotDateTime.isBefore(now)) {
                throw new ApiException("Cannot book a slot in the past", HttpStatus.BAD_REQUEST);
            }
        }

        for (Slot slot : slots) {
            slot.setIsAvailable(false);
        }
        slotRepository.saveAll(slots);

        String bookingCode = "BUFF-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTurf(turf);
        booking.setSlots(slots);
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingCode(bookingCode);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        List<Participant> participants = new ArrayList<>();
        
        if (request.getPlayers() != null && !request.getPlayers().isEmpty()) {
            // 1. Add Main User as Participant (from first element of players)
            BookingRequest.PlayerRequest mainReq = request.getPlayers().get(0);
            Participant mainParticipant = new Participant();
            mainParticipant.setBooking(booking);
            mainParticipant.setFullName(mainReq.getName() != null && !mainReq.getName().trim().isEmpty() ? mainReq.getName() : user.getUsername());
            mainParticipant.setAge(mainReq.getAge());
            mainParticipant.setGender(mainReq.getGender() != null ? mainReq.getGender() : "Male");
            mainParticipant.setPhone(mainReq.getContact() != null ? mainReq.getContact() : user.getPhoneNumber());
            mainParticipant.setIdHash(mainReq.getGovernmentId());
            mainParticipant.setVerified(true);
            participants.add(mainParticipant);

            // 2. Add Friends
            for (int i = 1; i < request.getPlayers().size(); i++) {
                BookingRequest.PlayerRequest p = request.getPlayers().get(i);
                Participant friend = new Participant();
                friend.setBooking(booking);
                friend.setFullName(p.getName());
                friend.setAge(p.getAge());
                friend.setGender(p.getGender());
                friend.setPhone(p.getContact());
                friend.setIdHash(p.getGovernmentId());
                friend.setVerified(false);
                participants.add(friend);
            }
        } else {
            // Fallback if players array is empty or null
            Participant mainParticipant = new Participant();
            mainParticipant.setBooking(booking);
            mainParticipant.setFullName(user.getUsername());
            mainParticipant.setAge(25); // Default
            mainParticipant.setGender("Male"); // Default
            mainParticipant.setPhone(user.getPhoneNumber());
            mainParticipant.setVerified(true);
            participants.add(mainParticipant);
        }
        booking.setParticipants(participants);

        // Save booking and cascade participants
        Booking savedBooking = bookingRepository.save(booking);

        // Generate QR Passes for each participant
        LocalTime minStartTime = slots.stream().map(Slot::getStartTime).min(LocalTime::compareTo).orElse(LocalTime.MIDNIGHT);
        LocalTime maxEndTime = slots.stream().map(Slot::getEndTime).max(LocalTime::compareTo).orElse(LocalTime.MIDNIGHT);
        
        LocalDateTime validFrom = LocalDateTime.of(request.getBookingDate(), minStartTime).minusMinutes(30);
        LocalDateTime validUntil = LocalDateTime.of(request.getBookingDate(), maxEndTime);
        Date expiresAt = Date.from(validUntil.atZone(ZoneId.systemDefault()).toInstant());

        for (Participant participant : savedBooking.getParticipants()) {
            String token = qrService.generateQrToken(participant.getId(), savedBooking.getId(), expiresAt);
            
            QrPass qrPass = new QrPass();
            qrPass.setParticipant(participant);
            qrPass.setBooking(savedBooking);
            qrPass.setValidFrom(validFrom);
            qrPass.setValidUntil(validUntil);
            qrPass.setScanStatus(QrPass.ScanStatus.PENDING);
            qrPass.setQrToken(token);
            
            participant.setQrPass(qrPass);
            qrPassRepository.save(qrPass);
        }

        String timeRange = minStartTime + " - " + maxEndTime;
        Double totalAmount = turf.getPricePerHour() * slots.size();

        // ── Send Email Confirmation (Free via Gmail SMTP) ──────────────────
        try {
            emailService.sendBookingConfirmation(
                    user.getEmail(),
                    user.getUsername(),
                    savedBooking.getBookingCode(),
                    turf.getName(),
                    turf.getLocation(),
                    turf.getSportType(),
                    savedBooking.getBookingDate().toString(),
                    timeRange,
                    totalAmount
            );
        } catch (Exception e) {
            System.err.println("❌ Failed to send booking confirmation email: " + e.getMessage());
        }

        // Clean up temporary Redis distributed locks on confirmed booking
        try {
            slotLockService.forceRelease(request.getSlotIds());
        } catch (Exception e) {
            System.err.println("⚠️ Non-critical: Failed to release Redis lock after booking: " + e.getMessage());
        }

        return savedBooking;
    }

    @Override
    @Transactional
    public Booking createBookingWithPayment(BookingRequest request, String email, String paymentId) {
        Booking booking = createBooking(request, email);
        booking.setPaymentId(paymentId);
        booking.setPaymentStatus("PAID");
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getMyBookings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return bookingRepository.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public void cancelBooking(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new ApiException("Not authorized to cancel this booking", HttpStatus.FORBIDDEN);
        }

        List<Slot> slots = booking.getSlots();
        for (Slot slot : slots) {
            slot.setIsAvailable(true);
        }
        slotRepository.saveAll(slots);

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // Expire all associated QR passes
        if (booking.getParticipants() != null) {
            for (Participant p : booking.getParticipants()) {
                if (p.getQrPass() != null) {
                    p.getQrPass().setScanStatus(QrPass.ScanStatus.CANCELLED);
                }
            }
        }

        bookingRepository.save(booking);
    }

    // Keep this method for legacy frontend support or general booking viewing
    @Override
    public Map<String, Object> verifyBooking(String code) {
        Booking booking = bookingRepository.findByBookingCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for code: " + code));
        
        // This method will be mostly superceded by the individual QR scanning, but we leave it for backwards compatibility

        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(istZone);
        LocalTime now = LocalTime.now(istZone);
        LocalDate bookingDate = booking.getBookingDate();
        List<Slot> slots = booking.getSlots();
        LocalTime startTime = slots.stream().map(Slot::getStartTime).min(LocalTime::compareTo).orElse(LocalTime.MIDNIGHT);
        LocalTime endTime = slots.stream().map(Slot::getEndTime).max(LocalTime::compareTo).orElse(LocalTime.MIDNIGHT);

        String validationStatus = "VALID";
        String validationMessage = "Ticket is valid. Entry approved!";

        // Security Validation Checks
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            validationStatus = "INVALID";
            validationMessage = "Entry Denied: This booking has been CANCELLED.";
        } else if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            validationStatus = "ALREADY_USED";
            validationMessage = "SECURITY WARNING: Replay Attack! This ticket has already been used and checked-in.";
        } else if (bookingDate.isBefore(today)) {
            validationStatus = "INVALID";
            validationMessage = "Entry Denied: This ticket is EXPIRED (Booking was for " + bookingDate + ").";
        } else if (bookingDate.isAfter(today)) {
            validationStatus = "FUTURE_BOOKING";
            validationMessage = "Entry Denied: This booking is for a future date: " + bookingDate + ".";
        } else {
            // It's today! Verify the slot timing with a 30-minute grace window before and after
            LocalTime graceStart = startTime.minusMinutes(30);
            LocalTime graceEnd = endTime.plusMinutes(30);

            boolean isTooEarly;
            boolean isExpired;

            if (graceStart.isBefore(graceEnd)) {
                // Normal same-day window
                isTooEarly = now.isBefore(graceStart);
                isExpired = now.isAfter(graceEnd);
            } else {
                // Window wraps around midnight (e.g. 23:00 to 01:00)
                isTooEarly = now.isBefore(graceStart) && now.isAfter(graceEnd);
                isExpired = now.isAfter(graceEnd) && now.isBefore(graceStart);
            }

            if (isTooEarly) {
                validationStatus = "TIME_CONFLICT";
                validationMessage = "Entry Denied: Too early. Slot starts at " + startTime + " (Grace entry starts at " + graceStart + ").";
            } else if (isExpired) {
                validationStatus = "TIME_CONFLICT";
                validationMessage = "Entry Denied: Expired. Slot ended at " + endTime + " (Grace entry closed at " + graceEnd + ").";
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingCode", booking.getBookingCode());
        result.put("status", booking.getStatus());
        result.put("turfName", booking.getTurf().getName());
        result.put("location", booking.getTurf().getLocation());
        result.put("address", booking.getTurf().getAddress());
        result.put("sportType", booking.getTurf().getSportType());
        result.put("slot", startTime + " - " + endTime);
        result.put("playerCount", booking.getParticipants() != null ? booking.getParticipants().size() : 0);
        result.put("bookingDate", bookingDate.toString());
        result.put("bookedBy", booking.getUser().getUsername());
        result.put("bookedByEmail", booking.getUser().getEmail());
        result.put("bookedByPhone", booking.getUser().getPhoneNumber());
        result.put("validationStatus", validationStatus);
        result.put("validationMessage", validationMessage);
        
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmCheckIn(String code) {
        Booking booking = bookingRepository.findByBookingCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for code: " + code));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ApiException("Cannot check in a cancelled booking", HttpStatus.BAD_REQUEST);
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new ApiException("Booking is already checked-in", HttpStatus.BAD_REQUEST);
        }

        booking.setStatus(Booking.BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Check-in confirmed successfully! Status updated to COMPLETED.");
        response.put("newStatus", booking.getStatus().toString());
        return response;
    }
}
