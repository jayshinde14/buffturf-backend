package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.OwnerAnalyticsDto;
import com.buffturf.buffturf_backend.dto.SlotResponseDto;
import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.service.OwnerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasAnyRole('TURF_OWNER', 'ADMIN')")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/turf")
    public ResponseEntity<Turf> getMyTurf(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ownerService.getMyTurf(userDetails.getUsername()));
    }

    @PutMapping("/turf")
    public ResponseEntity<Turf> updateMyTurf(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody Turf updatedTurf) {
        return ResponseEntity.ok(ownerService.updateMyTurf(userDetails.getUsername(), updatedTurf));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotResponseDto>> getMyTurfSlots(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ownerService.getMyTurfSlots(userDetails.getUsername(), date));
    }

    @PostMapping("/slots/generate")
    public ResponseEntity<List<Slot>> generateMyTurfSlots(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ownerService.generateMyTurfSlots(userDetails.getUsername(), date));
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<Map<String, String>> deleteMyTurfSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long slotId) {
        ownerService.deleteMyTurfSlot(userDetails.getUsername(), slotId);
        return ResponseEntity.ok(Map.of("message", "Slot successfully deleted"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getMyTurfBookings(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ownerService.getMyTurfBookings(userDetails.getUsername()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<OwnerAnalyticsDto> getMyTurfAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ownerService.getMyTurfAnalytics(userDetails.getUsername()));
    }
}
