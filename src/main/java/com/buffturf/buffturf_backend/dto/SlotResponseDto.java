package com.buffturf.buffturf_backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class SlotResponseDto {

    private Long id;
    private Long turfId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isAvailable;
    private String lockStatus; // "AVAILABLE", "HELD_BY_YOU", "HELD_BY_OTHER", "BOOKED"
    private Long lockExpiresInSeconds;

    public SlotResponseDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTurfId() { return turfId; }
    public void setTurfId(Long turfId) { this.turfId = turfId; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getLockStatus() { return lockStatus; }
    public void setLockStatus(String lockStatus) { this.lockStatus = lockStatus; }

    public Long getLockExpiresInSeconds() { return lockExpiresInSeconds; }
    public void setLockExpiresInSeconds(Long lockExpiresInSeconds) { this.lockExpiresInSeconds = lockExpiresInSeconds; }
}
