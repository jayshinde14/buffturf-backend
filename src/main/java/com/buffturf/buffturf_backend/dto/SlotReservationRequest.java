package com.buffturf.buffturf_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class SlotReservationRequest {

    @NotNull(message = "Turf ID is required")
    private Long turfId;

    @NotEmpty(message = "At least one slot must be selected")
    private List<Long> slotIds;

    private LocalDate bookingDate;

    public SlotReservationRequest() {}

    public Long getTurfId() { return turfId; }
    public void setTurfId(Long turfId) { this.turfId = turfId; }

    public List<Long> getSlotIds() { return slotIds; }
    public void setSlotIds(List<Long> slotIds) { this.slotIds = slotIds; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
}
