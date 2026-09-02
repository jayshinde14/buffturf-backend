package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.SlotReservationResponse;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.model.Slot;
import com.buffturf.buffturf_backend.repository.SlotRepository;
import com.buffturf.buffturf_backend.service.SlotLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SlotLockServiceImpl implements SlotLockService {

    private static final Logger log = LoggerFactory.getLogger(SlotLockServiceImpl.class);
    private static final String LOCK_KEY_PREFIX = "slot:lock:";

    @Value("${buffturf.slot.reservation.ttl.seconds:600}")
    private long defaultTtlSeconds;

    private final StringRedisTemplate redisTemplate;
    private final SlotRepository slotRepository;

    // In-memory fallback if Redis is temporarily offline in dev
    private final Map<Long, InMemoryLock> inMemoryFallbackLocks = new ConcurrentHashMap<>();

    private static class InMemoryLock {
        final String userEmail;
        final long expiresAtMillis;

        InMemoryLock(String userEmail, long expiresAtMillis) {
            this.userEmail = userEmail;
            this.expiresAtMillis = expiresAtMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    @Autowired
    public SlotLockServiceImpl(@Autowired(required = false) StringRedisTemplate redisTemplate,
                               SlotRepository slotRepository) {
        this.redisTemplate = redisTemplate;
        this.slotRepository = slotRepository;
    }

    @Override
    public SlotReservationResponse acquireHold(List<Long> slotIds, String userEmail, long ttlSeconds) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new ApiException("No slots provided for reservation", HttpStatus.BAD_REQUEST);
        }
        if (ttlSeconds <= 0) {
            ttlSeconds = defaultTtlSeconds;
        }

        // 1. Verify DB state: ensure slots exist and are not already booked
        List<Slot> slots = slotRepository.findAllById(slotIds);
        if (slots.size() != slotIds.size()) {
            throw new ApiException("One or more slots do not exist", HttpStatus.NOT_FOUND);
        }

        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        LocalDateTime now = LocalDateTime.now(istZone);
        for (Slot slot : slots) {
            if (!slot.getIsAvailable()) {
                throw new ApiException("Slot #" + slot.getId() + " is already permanently booked!", HttpStatus.CONFLICT);
            }
            LocalDateTime slotDateTime = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
            if (slotDateTime.isBefore(now)) {
                throw new ApiException("Cannot reserve a slot in the past", HttpStatus.BAD_REQUEST);
            }
        }

        // 2. Multi-slot atomic acquisition with automatic rollback
        List<Long> successfullyLockedSlots = new ArrayList<>();
        boolean hasConflict = false;
        String conflictMessage = "";

        for (Long slotId : slotIds) {
            boolean acquired = tryAcquireSingleSlot(slotId, userEmail, ttlSeconds);
            if (acquired) {
                successfullyLockedSlots.add(slotId);
            } else {
                hasConflict = true;
                conflictMessage = "Slot #" + slotId + " is currently in checkout by another customer. Please choose another slot or wait for the hold to expire.";
                break;
            }
        }

        // If any lock failed in the batch, ROLLBACK all previously locked slots in this attempt!
        if (hasConflict) {
            log.warn("Reservation conflict encountered for user {}. Rolling back {} acquired locks.", userEmail, successfullyLockedSlots.size());
            for (Long lockedId : successfullyLockedSlots) {
                releaseSingleSlot(lockedId, userEmail);
            }
            throw new ApiException(conflictMessage, HttpStatus.CONFLICT);
        }

        long expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000L);
        log.info("Successfully acquired Redis distributed hold on slots {} for user {} with TTL {}s", slotIds, userEmail, ttlSeconds);

        return new SlotReservationResponse(
                true,
                "Slots successfully reserved for " + (ttlSeconds / 60) + " minutes",
                slotIds,
                expiresAt,
                ttlSeconds
        );
    }

    @Override
    public void releaseHold(List<Long> slotIds, String userEmail) {
        if (slotIds == null || slotIds.isEmpty()) return;
        for (Long slotId : slotIds) {
            releaseSingleSlot(slotId, userEmail);
        }
        log.info("Released holds on slots {} for user {}", slotIds, userEmail);
    }

    @Override
    public void forceRelease(List<Long> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) return;
        for (Long slotId : slotIds) {
            forceReleaseSingleSlot(slotId);
        }
        log.info("Force-released Redis lock on slots {}", slotIds);
    }

    @Override
    public boolean validateUserHoldsLock(List<Long> slotIds, String userEmail) {
        if (slotIds == null || slotIds.isEmpty()) return false;
        for (Long slotId : slotIds) {
            String owner = getLockOwner(slotId);
            // If the slot is not locked or locked by someone else, validation fails
            if (owner == null || !owner.equalsIgnoreCase(userEmail)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getLockOwner(Long slotId) {
        if (redisTemplate != null) {
            try {
                return redisTemplate.opsForValue().get(LOCK_KEY_PREFIX + slotId);
            } catch (Exception e) {
                log.warn("Redis unreachable while reading lock for slot {}. Falling back to in-memory: {}", slotId, e.getMessage());
            }
        }
        InMemoryLock lock = inMemoryFallbackLocks.get(slotId);
        if (lock != null) {
            if (lock.isExpired()) {
                inMemoryFallbackLocks.remove(slotId);
                return null;
            }
            return lock.userEmail;
        }
        return null;
    }

    @Override
    public Long getLockTtlSeconds(Long slotId) {
        if (redisTemplate != null) {
            try {
                Long expire = redisTemplate.getExpire(LOCK_KEY_PREFIX + slotId);
                return (expire != null && expire > 0) ? expire : 0L;
            } catch (Exception e) {
                log.warn("Redis unreachable while reading TTL for slot {}: {}", slotId, e.getMessage());
            }
        }
        InMemoryLock lock = inMemoryFallbackLocks.get(slotId);
        if (lock != null) {
            if (lock.isExpired()) {
                inMemoryFallbackLocks.remove(slotId);
                return 0L;
            }
            return Math.max(0L, (lock.expiresAtMillis - System.currentTimeMillis()) / 1000L);
        }
        return 0L;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helper Methods for Single Slot Operations
    // ─────────────────────────────────────────────────────────────────────────────

    private boolean tryAcquireSingleSlot(Long slotId, String userEmail, long ttlSeconds) {
        String key = LOCK_KEY_PREFIX + slotId;
        if (redisTemplate != null) {
            try {
                // If user already owns this lock, renew it
                String currentOwner = redisTemplate.opsForValue().get(key);
                if (userEmail.equalsIgnoreCase(currentOwner)) {
                    redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
                    return true;
                }

                // Atomic SET key value NX EX ttlSeconds
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(key, userEmail, Duration.ofSeconds(ttlSeconds));
                return Boolean.TRUE.equals(success);
            } catch (Exception e) {
                log.warn("Redis unreachable on lock acquisition for slot {}. Using in-memory fallback: {}", slotId, e.getMessage());
            }
        }

        // In-memory fallback
        InMemoryLock existing = inMemoryFallbackLocks.get(slotId);
        long now = System.currentTimeMillis();
        if (existing == null || existing.isExpired() || existing.userEmail.equalsIgnoreCase(userEmail)) {
            inMemoryFallbackLocks.put(slotId, new InMemoryLock(userEmail, now + (ttlSeconds * 1000L)));
            return true;
        }
        return false;
    }

    private void releaseSingleSlot(Long slotId, String userEmail) {
        String key = LOCK_KEY_PREFIX + slotId;
        if (redisTemplate != null) {
            try {
                String currentOwner = redisTemplate.opsForValue().get(key);
                if (userEmail.equalsIgnoreCase(currentOwner)) {
                    redisTemplate.delete(key);
                }
                return;
            } catch (Exception e) {
                log.warn("Redis unreachable on lock release for slot {}: {}", slotId, e.getMessage());
            }
        }

        InMemoryLock lock = inMemoryFallbackLocks.get(slotId);
        if (lock != null && lock.userEmail.equalsIgnoreCase(userEmail)) {
            inMemoryFallbackLocks.remove(slotId);
        }
    }

    private void forceReleaseSingleSlot(Long slotId) {
        String key = LOCK_KEY_PREFIX + slotId;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Redis unreachable on force release for slot {}: {}", slotId, e.getMessage());
            }
        }
        inMemoryFallbackLocks.remove(slotId);
    }
}
