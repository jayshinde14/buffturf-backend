package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.SlotReservationRequest;
import com.buffturf.buffturf_backend.dto.SlotReservationResponse;
import com.buffturf.buffturf_backend.dto.SlotResponseDto;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.service.SlotLockService;
import com.buffturf.buffturf_backend.service.SlotService;
import jakarta.validation.Valid;
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
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;
    private final SlotLockService slotLockService;

    public SlotController(SlotService slotService, SlotLockService slotLockService) {
        this.slotService = slotService;
        this.slotLockService = slotLockService;
    }

    /**
     * Returns slots for the turf and date enriched with real-time Redis lock status & TTL
     */
    @GetMapping("/turfs/{turfId}/slots")
    public ResponseEntity<List<SlotResponseDto>> getSlotsByTurfAndDate(
            @PathVariable Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUserEmail = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(slotService.getEnrichedSlotsByTurfAndDate(turfId, date, currentUserEmail));
    }

    /**
     * Reserves/holds slots for 10 minutes (TTL) with distributed lock
     */
    @PostMapping("/slots/reserve")
    public ResponseEntity<SlotReservationResponse> reserveSlots(
            @Valid @RequestBody SlotReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SlotReservationResponse response = slotLockService.acquireHold(
                request.getSlotIds(),
                userDetails.getUsername(),
                600L // 10 minutes hold
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Releases user's hold on selected slots (e.g., when user abandons or changes slots)
     */
    @PostMapping("/slots/release")
    public ResponseEntity<Map<String, Object>> releaseSlots(
            @RequestBody SlotReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        slotLockService.releaseHold(request.getSlotIds(), userDetails.getUsername());
        return ResponseEntity.ok(Map.of("success", true, "message", "Slots released successfully"));
    }

    @PostMapping("/admin/turfs/{turfId}/slots/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Slot>> generateSlots(
            @PathVariable Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(slotService.generateSlots(turfId, date));
    }

    @DeleteMapping("/admin/slots/{slotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteSlot(@PathVariable Long slotId) {
        slotService.deleteSlot(slotId);
        return ResponseEntity.ok("Slot deleted");
    }
}

