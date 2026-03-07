package com.buffturf.buffturf_backend.repository;

import com.buffturf.buffturf_backend.model.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByTurfIdAndSlotDate(Long turfId, LocalDate slotDate);
    List<Slot> findByTurfIdAndSlotDateAndIsAvailable(
            Long turfId, LocalDate slotDate, Boolean isAvailable);
}