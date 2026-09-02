package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.TurfOwnerDto;
import com.buffturf.buffturf_backend.model.AuditLog;
import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.model.User;
import com.buffturf.buffturf_backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(adminService.getAllBookings());
    }

    @GetMapping("/bookings/today")
    public ResponseEntity<List<Booking>> getTodayBookings() {
        return ResponseEntity.ok(adminService.getTodayBookings());
    }

    @PutMapping("/bookings/{id}/cancel")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        adminService.cancelBookingByAdmin(id);
        return ResponseEntity.ok("Booking cancelled by admin");
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/earnings")
    public ResponseEntity<Map<String, Object>> getEarnings() {
        return ResponseEntity.ok(adminService.getEarnings());
    }

    @PutMapping("/users/{id}/ban")
    public ResponseEntity<Map<String, String>> toggleUserBan(@PathVariable Long id) {
        adminService.toggleUserBanStatus(id);
        return ResponseEntity.ok(Map.of("message", "User ban status toggled successfully"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

    @GetMapping("/owners")
    public ResponseEntity<List<TurfOwnerDto>> getAllTurfOwners() {
        return ResponseEntity.ok(adminService.getAllTurfOwners());
    }

    @PostMapping("/owners")
    public ResponseEntity<TurfOwnerDto> createTurfOwner(@RequestBody TurfOwnerDto dto) {
        return ResponseEntity.ok(adminService.createTurfOwner(dto));
    }
}

