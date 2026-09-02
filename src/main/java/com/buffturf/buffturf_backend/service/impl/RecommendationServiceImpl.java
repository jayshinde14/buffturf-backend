package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.service.RecommendationService;
import com.buffturf.buffturf_backend.service.TurfService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final TurfService turfService;
    private final RestTemplate restTemplate;

    // Hardcoded for MVP. In production, this should be in application.properties
    @org.springframework.beans.factory.annotation.Value("${ML_API_URL:http://localhost:8000/api/recommend}")
    private String PYTHON_SERVICE_URL;

    public RecommendationServiceImpl(TurfService turfService) {
        this.turfService = turfService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<Turf> getRecommendedTurfs(Long userId, Long turfId) {
        List<Turf> recommendedTurfs = new ArrayList<>();

        try {
            // Build the URL
            String url = PYTHON_SERVICE_URL + "?user_id=" + userId + "&turf_id=" + turfId;

            // Call the Python FastAPI Service
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("recommended_turfs")) {
                List<Integer> turfIds = (List<Integer>) response.get("recommended_turfs");

                // Fetch full Turf details from our local DB
                for (Integer id : turfIds) {
                    try {
                        Turf turf = turfService.getTurfById(id.longValue());
                        if (turf != null) {
                            recommendedTurfs.add(turf);
                        }
                    } catch (Exception e) {
                        // Ignore if a single turf fetch fails, skip it
                        System.err.println("Warning: Could not fetch recommended turf ID " + id);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: If Python service is down or fails (cold start), catch the error.
            System.err.println("Error calling AI Recommendation Service: " + e.getMessage());
            // We return an empty list here. Alternatively, could return top rated turfs.
        }

        return recommendedTurfs;
    }
}
