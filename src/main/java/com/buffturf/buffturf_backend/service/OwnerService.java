package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.dto.OwnerAnalyticsDto;
import com.buffturf.buffturf_backend.dto.SlotResponseDto;
import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.model.Turf;

import java.time.LocalDate;
import java.util.List;

public interface OwnerService {

    Turf getMyTurf(String ownerEmail);

    Turf updateMyTurf(String ownerEmail, Turf updatedTurf);

    List<SlotResponseDto> getMyTurfSlots(String ownerEmail, LocalDate date);

    List<Slot> generateMyTurfSlots(String ownerEmail, LocalDate date);

    void deleteMyTurfSlot(String ownerEmail, Long slotId);

    List<Booking> getMyTurfBookings(String ownerEmail);

    OwnerAnalyticsDto getMyTurfAnalytics(String ownerEmail);
}
