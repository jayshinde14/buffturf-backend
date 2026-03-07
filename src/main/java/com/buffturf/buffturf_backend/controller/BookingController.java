package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.BookingRequest;
import com.buffturf.buffturf_backend.model.*;
import com.buffturf.buffturf_backend.repository.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    public BookingController(BookingRepository bookingRepository,
                             TurfRepository turfRepository,
                             SlotRepository slotRepository,
                             UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.turfRepository = turfRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Turf turf = turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.getIsAvailable()) {
            return ResponseEntity.badRequest()
                    .body("Slot is already booked!");
        }

        slot.setIsAvailable(false);
        slotRepository.save(slot);

        String bookingCode = "BUFF-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();
        String qrCode = generateQRCode(bookingCode);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingCode(bookingCode);
        booking.setQrCode(qrCode);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        if (request.getPlayers() != null) {
            List<Player> players = new ArrayList<>();
            for (BookingRequest.PlayerRequest p : request.getPlayers()) {
                Player player = new Player();
                player.setBooking(booking);
                player.setName(p.getName());
                player.setAge(p.getAge());
                player.setGender(p.getGender());
                player.setContact(p.getContact());
                player.setGovernmentId(p.getGovernmentId());
                players.add(player);
            }
            booking.setPlayers(players);
        }

        return ResponseEntity.ok(bookingRepository.save(booking));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(
                bookingRepository.findByUserId(user.getId()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getUsername()
                .equals(userDetails.getUsername())) {
            return ResponseEntity.status(403)
                    .body("Not authorized to cancel this booking");
        }

        Slot slot = booking.getSlot();
        slot.setIsAvailable(true);
        slotRepository.save(slot);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return ResponseEntity.ok("Booking cancelled successfully");
    }

    @GetMapping("/verify/{code}")
    public ResponseEntity<?> verifyBooking(@PathVariable String code) {
        return bookingRepository.findByBookingCode(code)
                .map(booking -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", booking.getStatus());
                    result.put("turfName", booking.getTurf().getName());
                    result.put("slot", booking.getSlot().getStartTime()
                            + " - " + booking.getSlot().getEndTime());
                    result.put("playerCount",
                            booking.getPlayers() != null
                                    ? booking.getPlayers().size() : 0);
                    result.put("bookingDate", booking.getBookingDate());
                    result.put("bookedBy",
                            booking.getUser().getUsername());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String generateQRCode(String text) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(
                    text, BarcodeFormat.QR_CODE, 250, 250);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(
                    bitMatrix, "PNG", outputStream);
            byte[] qrBytes = outputStream.toByteArray();
            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(qrBytes);
        } catch (Exception e) {
            return null;
        }
    }
}