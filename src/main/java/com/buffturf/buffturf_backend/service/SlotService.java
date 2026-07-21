package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.model.Slot;
import java.time.LocalDate;
import java.util.List;

public interface SlotService {
    List<Slot> getSlotsByTurfAndDate(Long turfId, LocalDate date);
    Slot getSlotById(Long id);
    Slot createSlot(Slot slot);
    List<Slot> createBatchSlots(Long turfId, List<Slot> slots);
    void deleteSlot(Long id);
    Slot updateSlotAvailability(Long id, boolean isAvailable);
    List<Slot> generateSlots(Long turfId, LocalDate date);
}
