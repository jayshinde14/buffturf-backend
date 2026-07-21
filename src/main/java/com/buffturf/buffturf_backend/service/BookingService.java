package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.dto.BookingRequest;
import com.buffturf.buffturf_backend.model.Booking;
import java.util.List;
import java.util.Map;

public interface BookingService {
    Booking createBooking(BookingRequest request, String email);
    Booking createBookingWithPayment(BookingRequest request, String email, String paymentId);
    List<Booking> getMyBookings(String email);
    void cancelBooking(Long bookingId, String email);
    Map<String, Object> verifyBooking(String code);
    Map<String, Object> confirmCheckIn(String code);
}
