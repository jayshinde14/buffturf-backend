package com.buffturf.buffturf_backend.dto;

public class PaymentRequest {
    private Long turfId;
    private Long slotId;
    private Double amount;

    public Long getTurfId() { return turfId; }
    public void setTurfId(Long turfId) { this.turfId = turfId; }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}