package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.repository.SlotRepository;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import com.buffturf.buffturf_backend.service.SlotService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final TurfRepository turfRepository;

    public SlotServiceImpl(SlotRepository slotRepository, TurfRepository turfRepository) {
        this.slotRepository = slotRepository;
        this.turfRepository = turfRepository;
    }

    @Override
    public List<Slot> getSlotsByTurfAndDate(Long turfId, LocalDate date) {
        return slotRepository.findByTurfIdAndSlotDate(turfId, date);
    }

    @Override
    public Slot getSlotById(Long id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + id));
    }

    @Override
    public Slot createSlot(Slot slot) {
        return slotRepository.save(slot);
    }

    @Override
    public List<Slot> createBatchSlots(Long turfId, List<Slot> slots) {
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new ResourceNotFoundException("Turf not found with id: " + turfId));
        
        return slots.stream().map(slot -> {
            slot.setTurf(turf);
            slot.setIsAvailable(true);
            return slotRepository.save(slot);
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteSlot(Long id) {
        Slot slot = getSlotById(id);
        slotRepository.delete(slot);
    }

    @Override
    public Slot updateSlotAvailability(Long id, boolean isAvailable) {
        Slot slot = getSlotById(id);
        slot.setIsAvailable(isAvailable);
        return slotRepository.save(slot);
    }

    @Override
    public List<Slot> generateSlots(Long turfId, LocalDate date) {
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new ResourceNotFoundException("Turf not found with id: " + turfId));

        List<Slot> generatedSlots = new java.util.ArrayList<>();
        java.time.LocalTime current = java.time.LocalTime.of(6, 0);
        java.time.LocalTime endHour = java.time.LocalTime.of(23, 0);

        while (current.isBefore(endHour)) {
            Slot slot = new Slot();
            slot.setTurf(turf);
            slot.setSlotDate(date);
            slot.setStartTime(current);
            slot.setEndTime(current.plusHours(1));
            slot.setIsAvailable(true);
            generatedSlots.add(slot);
            current = current.plusHours(1);
        }

        return slotRepository.saveAll(generatedSlots);
    }
}
