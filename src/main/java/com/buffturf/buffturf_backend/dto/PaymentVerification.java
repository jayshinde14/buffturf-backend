package com.buffturf.buffturf_backend.dto;

import java.util.List;

public class PaymentVerification {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private Long turfId;
    private List<Long> slotIds;
    private String bookingDate;
    private List<BookingRequest.PlayerRequest> players;

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }

    public Long getTurfId() { return turfId; }
    public void setTurfId(Long turfId) { this.turfId = turfId; }

    public List<Long> getSlotIds() { return slotIds; }
    public void setSlotIds(List<Long> slotIds) { this.slotIds = slotIds; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public List<BookingRequest.PlayerRequest> getPlayers() { return players; }
    public void setPlayers(List<BookingRequest.PlayerRequest> players) { this.players = players; }
}