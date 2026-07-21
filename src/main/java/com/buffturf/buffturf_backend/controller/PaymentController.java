package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.BookingRequest;
import com.buffturf.buffturf_backend.dto.PaymentRequest;
import com.buffturf.buffturf_backend.dto.PaymentVerification;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.service.BookingService;
import com.buffturf.buffturf_backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    public PaymentController(PaymentService paymentService, BookingService bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(request.getAmount()));
    }

    @PostMapping("/verify")
    public ResponseEntity<Booking> verifyAndBook(
            @RequestBody PaymentVerification request,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isValid = paymentService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            throw new ApiException("Payment verification failed!", HttpStatus.BAD_REQUEST);
        }

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setTurfId(request.getTurfId());
        bookingRequest.setSlotIds(request.getSlotIds());
        bookingRequest.setBookingDate(LocalDate.parse(request.getBookingDate()));
        bookingRequest.setPlayers(request.getPlayers());

        return ResponseEntity.ok(bookingService.createBookingWithPayment(
                bookingRequest, userDetails.getUsername(), request.getRazorpayPaymentId()));
    }
}
