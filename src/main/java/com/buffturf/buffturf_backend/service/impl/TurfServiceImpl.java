package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import com.buffturf.buffturf_backend.service.TurfService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TurfServiceImpl implements TurfService {

    private final TurfRepository turfRepository;

    public TurfServiceImpl(TurfRepository turfRepository) {
        this.turfRepository = turfRepository;
    }

    @Override
    public List<Turf> getAllTurfs() {
        return turfRepository.findAll();
    }

    @Override
    public Turf getTurfById(Long id) {
        return turfRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turf not found with id: " + id));
    }

    @Override
    public Turf createTurf(Turf turf) {
        return turfRepository.save(turf);
    }

    @Override
    public Turf updateTurf(Long id, Turf turf) {
        Turf existingTurf = getTurfById(id);
        existingTurf.setName(turf.getName());
        existingTurf.setLocation(turf.getLocation());
        existingTurf.setSportType(turf.getSportType());
        existingTurf.setPricePerHour(turf.getPricePerHour());
        existingTurf.setDescription(turf.getDescription());
        existingTurf.setPhotoUrls(turf.getPhotoUrls());
        return turfRepository.save(existingTurf);
    }

    @Override
    public void deleteTurf(Long id) {
        Turf turf = getTurfById(id);
        turfRepository.delete(turf);
    }

    @Override
    public List<Turf> searchTurfs(String location, String sportType) {
        if ((location == null || location.isEmpty()) &&
                (sportType == null || sportType.isEmpty())) {
            return turfRepository.findAll();
        } else if (location != null && !location.isEmpty() &&
                sportType != null && !sportType.isEmpty()) {
            return turfRepository.findByLocationContainingIgnoreCaseAndSportTypeIgnoreCase(location, sportType);
        } else if (location != null && !location.isEmpty()) {
            return turfRepository.findByLocationContainingIgnoreCase(location);
        } else {
            return turfRepository.findBySportTypeIgnoreCase(sportType);
        }
    }
}
