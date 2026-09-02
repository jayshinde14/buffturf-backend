package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.SlotReservationResponse;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotLockServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SlotRepository slotRepository;

    private SlotLockServiceImpl slotLockService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        slotLockService = new SlotLockServiceImpl(redisTemplate, slotRepository);
    }

    private Slot createFutureSlot(Long id, LocalTime start, LocalTime end) {
        Slot slot = new Slot();
        slot.setId(id);
        slot.setSlotDate(LocalDate.now().plusDays(1));
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setIsAvailable(true);
        return slot;
    }

    @Test
    void testAcquireHold_Success() {
        Slot slot1 = createFutureSlot(101L, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Slot slot2 = createFutureSlot(102L, LocalTime.of(11, 0), LocalTime.of(12, 0));

        when(slotRepository.findAllById(Arrays.asList(101L, 102L))).thenReturn(Arrays.asList(slot1, slot2));
        when(valueOperations.setIfAbsent(eq("slot:lock:101"), eq("user@test.com"), any(Duration.class))).thenReturn(true);
        when(valueOperations.setIfAbsent(eq("slot:lock:102"), eq("user@test.com"), any(Duration.class))).thenReturn(true);

        SlotReservationResponse response = slotLockService.acquireHold(Arrays.asList(101L, 102L), "user@test.com", 600L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getReservedSlotIds().size());
        assertEquals(600L, response.getTtlSeconds());
        assertTrue(response.getExpiresAt() > System.currentTimeMillis());
    }

    @Test
    void testAcquireHold_Conflict_RollsBackAcquiredLocks() {
        Slot slot1 = createFutureSlot(101L, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Slot slot2 = createFutureSlot(102L, LocalTime.of(11, 0), LocalTime.of(12, 0));

        when(slotRepository.findAllById(Arrays.asList(101L, 102L))).thenReturn(Arrays.asList(slot1, slot2));
        // Slot 1 is initially unowned, acquired successfully. Slot 2 is locked by someone else.
        lenient().when(valueOperations.get("slot:lock:101")).thenReturn(null, "userB@test.com");
        lenient().when(valueOperations.get("slot:lock:102")).thenReturn("other@test.com");
        when(valueOperations.setIfAbsent(eq("slot:lock:101"), eq("userB@test.com"), any(Duration.class))).thenReturn(true);
        when(valueOperations.setIfAbsent(eq("slot:lock:102"), eq("userB@test.com"), any(Duration.class))).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () ->
                slotLockService.acquireHold(Arrays.asList(101L, 102L), "userB@test.com", 600L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("currently in checkout by another customer"));

        // Verify slot 1 lock was rolled back and deleted
        verify(redisTemplate).delete("slot:lock:101");
    }

    @Test
    void testReleaseHold_DeletesOnlyIfOwnedByUser() {
        when(valueOperations.get("slot:lock:101")).thenReturn("user@test.com");

        slotLockService.releaseHold(List.of(101L), "user@test.com");

        verify(redisTemplate).delete("slot:lock:101");
    }
}
