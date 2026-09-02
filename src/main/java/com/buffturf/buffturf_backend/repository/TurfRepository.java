package com.buffturf.buffturf_backend.repository;

import com.buffturf.buffturf_backend.model.Turf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TurfRepository extends JpaRepository<Turf, Long> {
    List<Turf> findByLocationContainingIgnoreCase(String location);
    List<Turf> findBySportTypeIgnoreCase(String sportType);
    List<Turf> findByLocationContainingIgnoreCaseAndSportTypeIgnoreCase(
            String location, String sportType);
    Optional<Turf> findByOwnerId(Long ownerId);
    Optional<Turf> findByOwnerEmail(String email);
    List<Turf> findAllByOwnerEmail(String email);
}