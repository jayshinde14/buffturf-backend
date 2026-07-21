package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.BookingRequest;
import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.createBooking(request, userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.getMyBookings(userDetails.getUsername()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        bookingService.cancelBooking(id, userDetails.getUsername());
        return ResponseEntity.ok("Booking cancelled successfully");
    }

    @GetMapping("/verify/{code}")
    public ResponseEntity<Map<String, Object>> verifyBooking(@PathVariable String code) {
        return ResponseEntity.ok(bookingService.verifyBooking(code));
    }

    @PostMapping("/verify/{code}/check-in")
    public ResponseEntity<Map<String, Object>> confirmCheckIn(@PathVariable String code) {
        return ResponseEntity.ok(bookingService.confirmCheckIn(code));
    }
}
