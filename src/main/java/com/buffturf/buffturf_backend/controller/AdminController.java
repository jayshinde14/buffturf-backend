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
    @GetMapping("/earnings")
    public ResponseEntity<Map<String, Object>> getEarnings() {
        List<Booking> allBookings = bookingRepository.findAll();
        List<Booking> confirmed = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .collect(java.util.stream.Collectors.toList());

        // Total revenue
        double totalRevenue = confirmed.stream()
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

        // Today's revenue
        double todayRevenue = confirmed.stream()
                .filter(b -> b.getBookingDate() != null &&
                        b.getBookingDate().equals(LocalDate.now()))
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

        // This month's revenue
        double monthRevenue = confirmed.stream()
                .filter(b -> b.getBookingDate() != null &&
                        b.getBookingDate().getMonth() == LocalDate.now().getMonth() &&
                        b.getBookingDate().getYear() == LocalDate.now().getYear())
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

        // Per turf breakdown
        Map<String, Map<String, Object>> turfEarnings = new HashMap<>();
        for (Booking b : confirmed) {
            if (b.getTurf() == null) continue;
            String turfName = b.getTurf().getName();
            turfEarnings.putIfAbsent(turfName, new HashMap<>());
            Map<String, Object> turfData = turfEarnings.get(turfName);
            turfData.put("turfName", turfName);
            turfData.put("location", b.getTurf().getLocation());
            turfData.put("sportType", b.getTurf().getSportType());
            turfData.put("pricePerHour", b.getTurf().getPricePerHour());
            int count = (int) turfData.getOrDefault("bookingCount", 0) + 1;
            double earned = (double) turfData.getOrDefault("totalEarned", 0.0) + b.getTurf().getPricePerHour();
            turfData.put("bookingCount", count);
            turfData.put("totalEarned", earned);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalRevenue", totalRevenue);
        response.put("todayRevenue", todayRevenue);
        response.put("monthRevenue", monthRevenue);
        response.put("confirmedBookings", confirmed.size());
        response.put("turfBreakdown", turfEarnings.values());
        response.put("recentPayments", confirmed.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList()));
        return ResponseEntity.ok(response);
    }
}