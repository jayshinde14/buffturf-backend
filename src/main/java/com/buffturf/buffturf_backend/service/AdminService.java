package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.model.Booking;
import com.buffturf.buffturf_backend.model.User;
import java.util.List;
import java.util.Map;

public interface AdminService {
    Map<String, Object> getDashboardStats();
    List<Booking> getAllBookings();
    List<Booking> getTodayBookings();
    void cancelBookingByAdmin(Long id);
    List<User> getAllUsers();
    Map<String, Object> getEarnings();
    void toggleUserBanStatus(Long userId);
}
