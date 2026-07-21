package com.buffturf.buffturf_backend.service;

import com.buffturf.buffturf_backend.dto.AuthResponse;
import com.buffturf.buffturf_backend.dto.LoginRequest;
import com.buffturf.buffturf_backend.dto.RegisterRequest;
import com.buffturf.buffturf_backend.dto.ForgotPasswordRequest;
import com.buffturf.buffturf_backend.dto.VerifyOtpRequest;
import com.buffturf.buffturf_backend.dto.ResetPasswordRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void verifyOtp(VerifyOtpRequest request);
    void resetPassword(ResetPasswordRequest request);
}
