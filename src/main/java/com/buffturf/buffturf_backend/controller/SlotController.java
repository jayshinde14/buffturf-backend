package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.repository.SlotRepository;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotRepository slotRepository;
    private final TurfRepository turfRepository;

    public SlotController(SlotRepository slotRepository,
                          TurfRepository turfRepository) {
        this.slotRepository = slotRepository;
        this.turfRepository = turfRepository;
    }

    @GetMapping("/turfs/{turfId}/slots")
    public ResponseEntity<List<Slot>> getSlotsByTurfAndDate(
            @PathVariable Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(
                slotRepository.findByTurfIdAndSlotDate(turfId, date));
    }

    @PostMapping("/admin/turfs/{turfId}/slots/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Slot>> generateSlots(
            @PathVariable Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new RuntimeException("Turf not found"));

        List<Slot> generatedSlots = new ArrayList<>();
        LocalTime current = LocalTime.of(6, 0);
        LocalTime endHour = LocalTime.of(23, 0);

        while (current.isBefore(endHour)) {
            Slot slot = new Slot();
            slot.setTurf(turf);
            slot.setSlotDate(date);
            slot.setStartTime(current);
            slot.setEndTime(current.plusHours(1));
            slot.setIsAvailable(true);
            generatedSlots.add(slot);
            current = current.plusHours(1);
        }

        return ResponseEntity.ok(slotRepository.saveAll(generatedSlots));
    }
 
    @DeleteMapping("/admin/slots/{slotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteSlot(@PathVariable Long slotId) {
        slotRepository.deleteById(slotId);
        return ResponseEntity.ok("Slot deleted");
    }
}