package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.dto.SlotReservationResponse;
import java.util.List;

public interface SlotLockService {

    /**
     * Attempts to acquire an atomic distributed lock (hold) for the given slot IDs.
     * If any slot is already locked or booked, roll back any previously acquired locks
     * in this batch and return a failure response.
     */
    SlotReservationResponse acquireHold(List<Long> slotIds, String userEmail, long ttlSeconds);

    /**
     * Releases the hold on the given slots if and only if they are held by the specified user.
     */
    void releaseHold(List<Long> slotIds, String userEmail);

    /**
     * Forcefully removes the lock keys on the given slots (used when booking is confirmed in DB).
     */
    void forceRelease(List<Long> slotIds);

    /**
     * Validates whether the specified user currently holds an active lock on ALL the given slots.
     */
    boolean validateUserHoldsLock(List<Long> slotIds, String userEmail);

    /**
     * Returns the user email of whoever currently holds the lock on this slot, or null if unlocked.
     */
    String getLockOwner(Long slotId);

    /**
     * Returns the remaining time-to-live (in seconds) for the lock on this slot, or 0 if unlocked.
     */
    Long getLockTtlSeconds(Long slotId);
}
