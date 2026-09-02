package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<Turf>> getRecommendations(
            @RequestParam Long userId,
            @RequestParam Long turfId) {

        List<Turf> recommendations = recommendationService.getRecommendedTurfs(userId, turfId);
        return ResponseEntity.ok(recommendations);
    }
}
