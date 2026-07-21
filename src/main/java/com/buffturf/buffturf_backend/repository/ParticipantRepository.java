package com.buffturf.buffturf_backend.repository;

import com.buffturf.buffturf_backend.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
}
