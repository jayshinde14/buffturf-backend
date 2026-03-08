package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.BookingRequest;
import com.buffturf.buffturf_backend.dto.PaymentRequest;
import com.buffturf.buffturf_backend.dto.PaymentVerification;
import com.buffturf.buffturf_backend.model.*;
import com.buffturf.buffturf_backend.repository.*;
import com.buffturf.buffturf_backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public PaymentController(PaymentService paymentService,
                             TurfRepository turfRepository,
                             SlotRepository slotRepository,
                             UserRepository userRepository,
                             BookingRepository bookingRepository) {
        this.paymentService = paymentService;
        this.turfRepository = turfRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody PaymentRequest request) {
        try {
            Map<String, Object> order =
                    paymentService.createOrder(request.getAmount());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to create order: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAndBook(
            @RequestBody PaymentVerification request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verify payment signature
        boolean isValid = paymentService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body("Payment verification failed!");
        }

        // Create booking after payment verified
        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Turf turf = turfRepository.findById(request.getTurfId())
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.getIsAvailable()) {
            return ResponseEntity.badRequest()
                    .body("Slot already booked!");
        }

        slot.setIsAvailable(false);
        slotRepository.save(slot);

        String bookingCode = "BUFF-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrCode = generateQRCode(bookingCode);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(LocalDate.parse(request.getBookingDate()));
        booking.setBookingCode(bookingCode);
        booking.setQrCode(qrCode);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentId(request.getRazorpayPaymentId());
        booking.setPaymentStatus("PAID");

        return ResponseEntity.ok(bookingRepository.save(booking));
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
            return "data:image/png;base64," +
                    Base64.getEncoder().encodeToString(qrBytes);
        } catch (Exception e) {
            return null;
        }
    }
}