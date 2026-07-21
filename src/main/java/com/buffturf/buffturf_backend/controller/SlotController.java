package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/turfs/{turfId}/slots")
    public ResponseEntity<List<Slot>> getSlotsByTurfAndDate(
            @PathVariable Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(slotService.getSlotsByTurfAndDate(turfId, date));
    }

    @PostMapping("/admin/turfs/{turfId}/slots/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Slot>> generateSlots(
            @PathVariable Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(slotService.generateSlots(turfId, date));
    }

    @DeleteMapping("/admin/slots/{slotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteSlot(@PathVariable Long slotId) {
        slotService.deleteSlot(slotId);
        return ResponseEntity.ok("Slot deleted");
    }
}
