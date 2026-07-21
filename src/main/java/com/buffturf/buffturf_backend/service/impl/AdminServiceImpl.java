package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.model.User;
import com.buffturf.buffturf_backend.repository.BookingRepository;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import com.buffturf.buffturf_backend.repository.UserRepository;
import com.buffturf.buffturf_backend.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;

    public AdminServiceImpl(UserRepository userRepository,
                            TurfRepository turfRepository,
                            BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.turfRepository = turfRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalTurfs", turfRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("todayBookings", bookingRepository.findByBookingDate(LocalDate.now()).size());
        return stats;
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public List<Booking> getTodayBookings() {
        return bookingRepository.findByBookingDate(LocalDate.now());
    }

    @Override
    @Transactional
    public void cancelBookingByAdmin(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // Expire all associated QR passes
        if (booking.getParticipants() != null) {
            for (com.buffturf.buffturf_backend.model.Participant p : booking.getParticipants()) {
                if (p.getQrPass() != null) {
                    p.getQrPass().setScanStatus(com.buffturf.buffturf_backend.model.QrPass.ScanStatus.CANCELLED);
                }
            }
        }

        bookingRepository.save(booking);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Map<String, Object> getEarnings() {
        List<Booking> allBookings = bookingRepository.findAll();
        List<Booking> confirmed = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .collect(Collectors.toList());

        double totalRevenue = confirmed.stream()
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

        double todayRevenue = confirmed.stream()
                .filter(b -> b.getBookingDate() != null && b.getBookingDate().equals(LocalDate.now()))
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

        double monthRevenue = confirmed.stream()
                .filter(b -> b.getBookingDate() != null &&
                        b.getBookingDate().getMonth() == LocalDate.now().getMonth() &&
                        b.getBookingDate().getYear() == LocalDate.now().getYear())
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

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
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public void toggleUserBanStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot ban an admin user!");
        }
        
        user.setBanned(!user.isBanned());
        userRepository.save(user);
    }
}
