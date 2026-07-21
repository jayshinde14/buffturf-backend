package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.service.TurfService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TurfController {

    private final TurfService turfService;

    public TurfController(TurfService turfService) {
        this.turfService = turfService;
    }

    @GetMapping("/turfs/search")
    public ResponseEntity<List<Turf>> searchTurfs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String sportType) {
        return ResponseEntity.ok(turfService.searchTurfs(location, sportType));
    }

    @GetMapping("/turfs/{id}")
    public ResponseEntity<Turf> getTurfById(@PathVariable Long id) {
        return ResponseEntity.ok(turfService.getTurfById(id));
    }

    @GetMapping("/turfs")
    public ResponseEntity<List<Turf>> getAllTurfs() {
        return ResponseEntity.ok(turfService.getAllTurfs());
    }

    @PostMapping("/admin/turfs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Turf> addTurf(@RequestBody Turf turf) {
        return ResponseEntity.ok(turfService.createTurf(turf));
    }

    @PutMapping("/admin/turfs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Turf> updateTurf(@PathVariable Long id,
                                           @RequestBody Turf updatedTurf) {
        return ResponseEntity.ok(turfService.updateTurf(id, updatedTurf));
    }

    @DeleteMapping("/admin/turfs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteTurf(@PathVariable Long id) {
        turfService.deleteTurf(id);
        return ResponseEntity.ok("Turf deleted successfully");
    }
}
