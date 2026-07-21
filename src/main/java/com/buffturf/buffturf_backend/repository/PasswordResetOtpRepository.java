package com.buffturf.buffturf_backend.repository;

import com.buffturf.buffturf_backend.model.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    List<PasswordResetOtp> findByEmailOrderByCreatedAtDesc(String email);
}
