package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.model.QrPass;
import com.buffturf.buffturf_backend.model.Participant;
import com.buffturf.buffturf_backend.repository.QrPassRepository;
import com.buffturf.buffturf_backend.service.QrService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "http://localhost:3000")
public class QrController {

    private final QrService qrService;
    private final QrPassRepository qrPassRepository;

    public QrController(QrService qrService, QrPassRepository qrPassRepository) {
        this.qrService = qrService;
        this.qrPassRepository = qrPassRepository;
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanQr(@RequestBody Map<String, String> payload) {
        String token = payload.get("qrToken");
        Map<String, Object> response = new HashMap<>();

        if (token == null || token.isEmpty()) {
            response.put("error", "QR token is missing");
            return ResponseEntity.badRequest().body(response);
        }

        Claims claims = qrService.validateQrToken(token);
        if (claims == null) {
            response.put("error", "Invalid or tampered QR token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Long participantId = claims.get("participantId", Long.class);

        Optional<QrPass> qrPassOpt = qrPassRepository.findByQrToken(token);
        if (qrPassOpt.isEmpty()) {
            response.put("error", "QR pass not found in system");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        QrPass qrPass = qrPassOpt.get();
        Participant participant = qrPass.getParticipant();

        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        // Validate time
        if (now.isBefore(qrPass.getValidFrom())) {
            response.put("error", "QR pass not active yet. Active from: " + qrPass.getValidFrom());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (now.isAfter(qrPass.getValidUntil())) {
            qrPass.setScanStatus(QrPass.ScanStatus.EXPIRED);
            qrPassRepository.save(qrPass);
            response.put("error", "QR pass has expired");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Validate state
        if (qrPass.getScanStatus() == QrPass.ScanStatus.CHECKED_IN) {
            response.put("error", "Participant already checked in at " + qrPass.getScannedAt());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (qrPass.getScanStatus() == QrPass.ScanStatus.CANCELLED) {
            response.put("error", "Booking was cancelled");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Check in successful
        qrPass.setScanStatus(QrPass.ScanStatus.CHECKED_IN);
        qrPass.setScannedAt(now);
        qrPassRepository.save(qrPass);

        response.put("success", true);
        response.put("message", "Entry Completed");
        response.put("participantName", participant.getFullName());
        response.put("bookingSlot", qrPass.getValidFrom().toLocalTime() + " - " + qrPass.getValidUntil().toLocalTime());
        response.put("verified", participant.getVerified());
        
        return ResponseEntity.ok(response);
    }
}
