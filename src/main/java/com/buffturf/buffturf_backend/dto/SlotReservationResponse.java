package com.buffturf.buffturf_backend.dto;

import java.util.List;

public class SlotReservationResponse {

    private boolean success;
    private String message;
    private List<Long> reservedSlotIds;
    private Long expiresAt; // Epoch timestamp (ms)
    private Long ttlSeconds;

    public SlotReservationResponse() {}

    public SlotReservationResponse(boolean success, String message, List<Long> reservedSlotIds, Long expiresAt, Long ttlSeconds) {
        this.success = success;
        this.message = message;
        this.reservedSlotIds = reservedSlotIds;
        this.expiresAt = expiresAt;
        this.ttlSeconds = ttlSeconds;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Long> getReservedSlotIds() { return reservedSlotIds; }
    public void setReservedSlotIds(List<Long> reservedSlotIds) { this.reservedSlotIds = reservedSlotIds; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public Long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}
