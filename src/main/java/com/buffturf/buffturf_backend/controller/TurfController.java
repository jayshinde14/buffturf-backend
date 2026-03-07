package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class TurfController {

    private final TurfRepository turfRepository;

    public TurfController(TurfRepository turfRepository) {
        this.turfRepository = turfRepository;
    }

    @GetMapping("/turfs/search")
    public ResponseEntity<List<Turf>> searchTurfs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String sportType) {

        List<Turf> turfs;

        if ((location == null || location.isEmpty()) &&
                (sportType == null || sportType.isEmpty())) {
            turfs = turfRepository.findAll();
        } else if (location != null && !location.isEmpty() &&
                sportType != null && !sportType.isEmpty()) {
            turfs = turfRepository
                    .findByLocationContainingIgnoreCaseAndSportTypeIgnoreCase(
                            location, sportType);
        } else if (location != null && !location.isEmpty()) {
            turfs = turfRepository
                    .findByLocationContainingIgnoreCase(location);
        } else {
            turfs = turfRepository.findBySportTypeIgnoreCase(sportType);
        }

        return ResponseEntity.ok(turfs);
    }

    @GetMapping("/turfs/{id}")
    public ResponseEntity<Turf> getTurfById(@PathVariable Long id) {
        return turfRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/turfs")
    public ResponseEntity<List<Turf>> getAllTurfs() {
        return ResponseEntity.ok(turfRepository.findAll());
    }

    @PostMapping("/admin/turfs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Turf> addTurf(@RequestBody Turf turf) {
        return ResponseEntity.ok(turfRepository.save(turf));
    }

    @PutMapping("/admin/turfs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Turf> updateTurf(@PathVariable Long id,
                                           @RequestBody Turf updatedTurf) {
        return turfRepository.findById(id).map(turf -> {
            turf.setName(updatedTurf.getName());
            turf.setLocation(updatedTurf.getLocation());
            turf.setAddress(updatedTurf.getAddress());
            turf.setSportType(updatedTurf.getSportType());
            turf.setPricePerHour(updatedTurf.getPricePerHour());
            turf.setDescription(updatedTurf.getDescription());
            turf.setName(updatedTurf.getOpenTime());
            turf.setCloseTime(updatedTurf.getCloseTime());
            return ResponseEntity.ok(turfRepository.save(turf));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/turfs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteTurf(@PathVariable Long id) {
        if (!turfRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        turfRepository.deleteById(id);
        return ResponseEntity.ok("Turf deleted successfully");
    }
}