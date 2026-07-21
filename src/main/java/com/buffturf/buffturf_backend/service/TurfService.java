package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.model.Turf;
import java.util.List;

public interface TurfService {
    List<Turf> getAllTurfs();
    Turf getTurfById(Long id);
    Turf createTurf(Turf turf);
    Turf updateTurf(Long id, Turf turf);
    void deleteTurf(Long id);
    List<Turf> searchTurfs(String location, String sportType);
}
