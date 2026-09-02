package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.model.Turf;
import java.util.List;

public interface RecommendationService {
    List<Turf> getRecommendedTurfs(Long userId, Long turfId);
}
