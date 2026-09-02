package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.TurfOwnerDto;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.AuditLog;
import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.model.User;
import com.buffturf.buffturf_backend.repository.AuditLogRepository;
import com.buffturf.buffturf_backend.repository.BookingRepository;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import com.buffturf.buffturf_backend.repository.UserRepository;
import com.buffturf.buffturf_backend.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(UserRepository userRepository,
                            TurfRepository turfRepository,
                            BookingRepository bookingRepository,
                            AuditLogRepository auditLogRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.turfRepository = turfRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalTurfs", turfRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("todayBookings", bookingRepository.findByBookingDate(LocalDate.now(istZone)).size());
        return stats;
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public List<Booking> getTodayBookings() {
        return bookingRepository.findByBookingDate(LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")));
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
        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(istZone);
        List<Booking> allBookings = bookingRepository.findAll();
        List<Booking> confirmed = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .collect(Collectors.toList());

        double totalRevenue = confirmed.stream()
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

        double todayRevenue = confirmed.stream()
                .filter(b -> b.getBookingDate() != null && b.getBookingDate().equals(today))
                .mapToDouble(b -> b.getTurf() != null ? b.getTurf().getPricePerHour() : 0)
                .sum();

        double monthRevenue = confirmed.stream()
                .filter(b -> b.getBookingDate() != null &&
                        b.getBookingDate().getMonth() == today.getMonth() &&
                        b.getBookingDate().getYear() == today.getYear())
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

    @Override
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<TurfOwnerDto> getAllTurfOwners() {
        List<User> owners = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.TURF_OWNER)
                .collect(Collectors.toList());

        List<Turf> allTurfs = turfRepository.findAll();

        return owners.stream().map(owner -> {
            Optional<Turf> assignedTurf = allTurfs.stream()
                    .filter(t -> t.getOwner() != null && t.getOwner().getId().equals(owner.getId()))
                    .findFirst();

            TurfOwnerDto dto = new TurfOwnerDto();
            dto.setUserId(owner.getId());
            dto.setUsername(owner.getUsername());
            dto.setEmail(owner.getEmail());
            dto.setPhoneNumber(owner.getPhoneNumber());

            assignedTurf.ifPresent(turf -> {
                dto.setTurfId(turf.getId());
                dto.setTurfName(turf.getName());
            });

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TurfOwnerDto createTurfOwner(TurfOwnerDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ApiException("User already exists with email: " + dto.getEmail(), HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ApiException("Username already taken: " + dto.getUsername(), HttpStatus.BAD_REQUEST);
        }

        User owner = new User();
        owner.setUsername(dto.getUsername());
        owner.setEmail(dto.getEmail());
        owner.setPassword(passwordEncoder.encode(dto.getPassword() != null ? dto.getPassword() : "owner123"));
        owner.setPhoneNumber(dto.getPhoneNumber());
        owner.setRole(User.Role.TURF_OWNER);
        User savedOwner = userRepository.save(owner);

        String turfName = null;
        if (dto.getTurfId() != null) {
            Turf turf = turfRepository.findById(dto.getTurfId())
                    .orElseThrow(() -> new ResourceNotFoundException("Turf not found with id: " + dto.getTurfId()));
            turf.setOwner(savedOwner);
            turfRepository.save(turf);
            turfName = turf.getName();

            auditLogRepository.save(new AuditLog(
                    turf.getId(),
                    turf.getName(),
                    "admin",
                    "OWNER_ASSIGNED",
                    String.format("Assigned new owner '%s' (%s) to turf '%s'", savedOwner.getUsername(), savedOwner.getEmail(), turf.getName())
            ));
        }

        return new TurfOwnerDto(
                savedOwner.getId(),
                savedOwner.getUsername(),
                savedOwner.getEmail(),
                savedOwner.getPhoneNumber(),
                dto.getTurfId(),
                turfName
        );
    }
}
