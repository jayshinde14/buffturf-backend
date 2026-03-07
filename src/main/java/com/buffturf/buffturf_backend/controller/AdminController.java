package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.repository.BookingRepository;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import com.buffturf.buffturf_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buffturf.buffturf_backend.model.User;
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;

    public AdminController(UserRepository userRepository,
                           TurfRepository turfRepository,
                           BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.turfRepository = turfRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalTurfs", turfRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("todayBookings",
                bookingRepository
                        .findByBookingDate(LocalDate.now()).size());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingRepository.findAll());
    }

    @GetMapping("/bookings/today")
    public ResponseEntity<List<Booking>> getTodayBookings() {
        return ResponseEntity.ok(
                bookingRepository.findByBookingDate(LocalDate.now()));
    }

    @PutMapping("/bookings/{id}/cancel")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found"));
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return ResponseEntity.ok("Booking cancelled by admin");
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}