package com.buffturf.buffturf_backend.repository;

import com.buffturf.buffturf_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByBookingDate(LocalDate date);
}