package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.SlotResponseDto;
import com.buffturf.buffturf_backend.exception.ResourceNotFoundException;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.model.Turf;
import com.buffturf.buffturf_backend.repository.SlotRepository;
import com.buffturf.buffturf_backend.repository.TurfRepository;
import com.buffturf.buffturf_backend.service.SlotLockService;
import com.buffturf.buffturf_backend.service.SlotService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final TurfRepository turfRepository;
    private final SlotLockService slotLockService;

    public SlotServiceImpl(SlotRepository slotRepository,
                           TurfRepository turfRepository,
                           SlotLockService slotLockService) {
        this.slotRepository = slotRepository;
        this.turfRepository = turfRepository;
        this.slotLockService = slotLockService;
    }

    private static final java.time.ZoneId IST_ZONE = java.time.ZoneId.of("Asia/Kolkata");

    @Override
    public List<Slot> getSlotsByTurfAndDate(Long turfId, LocalDate date) {
        List<Slot> slots = slotRepository.findByTurfIdAndSlotDate(turfId, date);
        if (slots.isEmpty() && date != null && !date.isBefore(LocalDate.now(IST_ZONE))) {
            slots = generateSlots(turfId, date);
        }
        return slots;
    }

    @Override
    public List<SlotResponseDto> getEnrichedSlotsByTurfAndDate(Long turfId, LocalDate date, String currentUserEmail) {
        List<Slot> slots = slotRepository.findByTurfIdAndSlotDate(turfId, date);
        if (slots.isEmpty() && date != null && !date.isBefore(LocalDate.now(IST_ZONE))) {
            slots = generateSlots(turfId, date);
        }

        return slots.stream().map(slot -> {
            SlotResponseDto dto = new SlotResponseDto();
            dto.setId(slot.getId());
            dto.setTurfId(turfId);
            dto.setSlotDate(slot.getSlotDate());
            dto.setStartTime(slot.getStartTime());
            dto.setEndTime(slot.getEndTime());
            dto.setIsAvailable(slot.getIsAvailable());

            if (!slot.getIsAvailable()) {
                dto.setLockStatus("BOOKED");
                dto.setLockExpiresInSeconds(0L);
            } else {
                String lockOwner = slotLockService.getLockOwner(slot.getId());
                if (lockOwner != null) {
                    if (currentUserEmail != null && lockOwner.equalsIgnoreCase(currentUserEmail)) {
                        dto.setLockStatus("HELD_BY_YOU");
                    } else {
                        dto.setLockStatus("HELD_BY_OTHER");
                    }
                    dto.setLockExpiresInSeconds(slotLockService.getLockTtlSeconds(slot.getId()));
                } else {
                    dto.setLockStatus("AVAILABLE");
                    dto.setLockExpiresInSeconds(0L);
                }
            }
            return dto;
        }).collect(Collectors.toList());
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
        java.time.LocalTime current = parseTimeSafe(turf.getOpenTime(), java.time.LocalTime.of(6, 0));
        java.time.LocalTime endHour = parseTimeSafe(turf.getCloseTime(), java.time.LocalTime.of(23, 0));

        List<Slot> existingSlots = slotRepository.findByTurfIdAndSlotDate(turfId, date);
        
        // In case closeTime is earlier or equal to openTime (e.g. 00:00 midnight), default to 23:00
        if (!endHour.isAfter(current)) {
            endHour = java.time.LocalTime.of(23, 0);
        }

        while (current.isBefore(endHour)) {
            final java.time.LocalTime slotTime = current;
            boolean exists = existingSlots.stream()
                    .anyMatch(s -> s.getStartTime().equals(slotTime));
                    
            if (!exists) {
                Slot slot = new Slot();
                slot.setTurf(turf);
                slot.setSlotDate(date);
                slot.setStartTime(current);
                slot.setEndTime(current.plusHours(1));
                slot.setIsAvailable(true);
                generatedSlots.add(slot);
            }
            current = current.plusHours(1);
        }

        return slotRepository.saveAll(generatedSlots);
    }

    private java.time.LocalTime parseTimeSafe(String timeStr, java.time.LocalTime defaultTime) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return defaultTime;
        }
        try {
            timeStr = timeStr.trim();
            if (timeStr.length() == 5) {
                return java.time.LocalTime.parse(timeStr);
            }
            if (timeStr.length() >= 8) {
                return java.time.LocalTime.parse(timeStr.substring(0, 8));
            }
            return java.time.LocalTime.parse(timeStr);
        } catch (Exception e) {
            return defaultTime;
        }
    }
}
