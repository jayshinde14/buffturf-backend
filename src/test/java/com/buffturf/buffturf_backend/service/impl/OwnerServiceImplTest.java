package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.OwnerAnalyticsDto;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.model.*;
import com.buffturf.buffturf_backend.repository.*;
import com.buffturf.buffturf_backend.service.SlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerServiceImplTest {

    @Mock
    private TurfRepository turfRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SlotService slotService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private OwnerServiceImpl ownerService;

    private User testOwner;
    private Turf testTurf;

    @BeforeEach
    void setUp() {
        testOwner = new User();
        testOwner.setId(10L);
        testOwner.setUsername("turf_owner");
        testOwner.setEmail("owner@arena.com");
        testOwner.setRole(User.Role.TURF_OWNER);

        testTurf = new Turf();
        testTurf.setId(1L);
        testTurf.setName("Arena Sports Hub");
        testTurf.setLocation("Koramangala");
        testTurf.setPricePerHour(1200.0);
        testTurf.setSportType("Football");
        testTurf.setOwner(testOwner);
    }

    @Test
    void testGetMyTurf_Success() {
        when(turfRepository.findByOwnerEmail("owner@arena.com")).thenReturn(Optional.of(testTurf));

        Turf result = ownerService.getMyTurf("owner@arena.com");

        assertNotNull(result);
        assertEquals("Arena Sports Hub", result.getName());
        assertEquals(1200.0, result.getPricePerHour());
    }

    @Test
    void testUpdateMyTurf_CreatesAuditLog() {
        when(turfRepository.findByOwnerEmail("owner@arena.com")).thenReturn(Optional.of(testTurf));
        when(turfRepository.save(any(Turf.class))).thenReturn(testTurf);

        Turf updateData = new Turf();
        updateData.setPricePerHour(1500.0);
        updateData.setName("Arena Sports Hub Pro");

        Turf updated = ownerService.updateMyTurf("owner@arena.com", updateData);

        assertEquals(1500.0, updated.getPricePerHour());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testGetMyTurfAnalytics_CalculatesMRRAndPayouts() {
        when(turfRepository.findByOwnerEmail("owner@arena.com")).thenReturn(Optional.of(testTurf));

        Slot slot = new Slot();
        slot.setId(101L);
        slot.setTurf(testTurf);
        slot.setStartTime(LocalTime.of(18, 0));
        slot.setEndTime(LocalTime.of(19, 0));
        slot.setIsAvailable(false);

        Booking booking = new Booking();
        booking.setId(501L);
        booking.setTurf(testTurf);
        booking.setBookingDate(LocalDate.now());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setAmountPaid(2000.0);
        booking.setSlots(List.of(slot));

        when(bookingRepository.findAll()).thenReturn(List.of(booking));
        when(slotRepository.findAll()).thenReturn(List.of(slot));

        OwnerAnalyticsDto analytics = ownerService.getMyTurfAnalytics("owner@arena.com");

        assertNotNull(analytics);
        assertEquals(2000.0, analytics.getTotalRevenue());
        assertEquals(2000.0, analytics.getMonthlyRevenue());
        assertEquals(200.0, analytics.getPlatformFeeAmount()); // 10% platform fee
        assertEquals(1800.0, analytics.getNetPayoutAmount()); // 90% net settlement payout
        assertEquals(100.0, analytics.getOccupancyRatePercent());
        assertEquals(1, analytics.getConfirmedBookings());
    }

    @Test
    void testDeleteSlot_DifferentTurf_Forbidden() {
        when(turfRepository.findByOwnerEmail("owner@arena.com")).thenReturn(Optional.of(testTurf));

        Turf anotherTurf = new Turf();
        anotherTurf.setId(99L);

        Slot slotFromOtherTurf = new Slot();
        slotFromOtherTurf.setId(888L);
        slotFromOtherTurf.setTurf(anotherTurf);
        slotFromOtherTurf.setIsAvailable(true);

        when(slotService.getSlotById(888L)).thenReturn(slotFromOtherTurf);

        ApiException ex = assertThrows(ApiException.class, () ->
                ownerService.deleteMyTurfSlot("owner@arena.com", 888L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
