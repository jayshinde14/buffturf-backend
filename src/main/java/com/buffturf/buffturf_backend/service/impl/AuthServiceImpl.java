package com.buffturf.buffturf_backend.service.impl;

import com.buffturf.buffturf_backend.dto.AuthResponse;
import com.buffturf.buffturf_backend.dto.LoginRequest;
import com.buffturf.buffturf_backend.dto.RegisterRequest;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.model.User;
import com.buffturf.buffturf_backend.repository.UserRepository;
import com.buffturf.buffturf_backend.security.CustomUserDetailsService;
import com.buffturf.buffturf_backend.security.JwtUtils;
import com.buffturf.buffturf_backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.buffturf.buffturf_backend.dto.ForgotPasswordRequest;
import com.buffturf.buffturf_backend.dto.VerifyOtpRequest;
import com.buffturf.buffturf_backend.dto.ResetPasswordRequest;
import com.buffturf.buffturf_backend.model.PasswordResetOtp;
import com.buffturf.buffturf_backend.repository.PasswordResetOtpRepository;
import com.buffturf.buffturf_backend.service.EmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;
    private final EmailService emailService;
    private final PasswordResetOtpRepository otpRepository;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           CustomUserDetailsService customUserDetailsService,
                           EmailService emailService,
                           PasswordResetOtpRepository otpRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.customUserDetailsService = customUserDetailsService;
        this.emailService = emailService;
        this.otpRepository = otpRepository;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String phoneNumber = request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : "";

        // Backend strict email domain validation to prevent fake/typo emails
        java.util.List<String> allowedDomains = java.util.Arrays.asList(
                "@gmail.com", "@yahoo.com", "@outlook.com", "@hotmail.com", "@icloud.com", "@protonmail.com"
        );
        boolean isAllowedDomain = allowedDomains.stream().anyMatch(email::endsWith);
        if (!isAllowedDomain) {
            throw new ApiException("Invalid email provider! Please use a standard email provider like @gmail.com", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByUsername(username)) {
            throw new ApiException("Username already taken!", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email already registered!", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ApiException("Phone number is already associated with another account!", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(phoneNumber);
        user.setRole(User.Role.USER);
        userRepository.save(user);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtils.generateToken(userDetails);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Registration successful!");
        return response;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email, request.getPassword())
            );
        } catch (Exception e) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        String token = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.isBanned()) {
            throw new ApiException("Your account has been banned.", HttpStatus.FORBIDDEN);
        }

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Login successful!");
        return response;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Security: Don't reveal if account exists. If it doesn't, just return silently.
        if (!userRepository.existsByEmail(email)) {
            return;
        }

        // Generate 6 digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        System.out.println("========== DEBUG OTP ==========");
        System.out.println("OTP for " + email + " is: " + otp);
        System.out.println("===============================");
        
        PasswordResetOtp resetOtp = new PasswordResetOtp();
        resetOtp.setEmail(email);
        resetOtp.setOtpHash(passwordEncoder.encode(otp)); // Hash the OTP
        resetOtp.setExpiryTime(LocalDateTime.now().plusMinutes(15));
        resetOtp.setUsed(false);
        
        otpRepository.save(resetOtp);
        emailService.sendPasswordResetOtp(email, otp);
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        List<PasswordResetOtp> otps = otpRepository.findByEmailOrderByCreatedAtDesc(email);
        if (otps.isEmpty()) {
            throw new ApiException("No pending OTP request found for this email.", HttpStatus.BAD_REQUEST);
        }

        PasswordResetOtp latestOtp = otps.get(0);

        if (latestOtp.isUsed()) {
            throw new ApiException("This OTP has already been used.", HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.now().isAfter(latestOtp.getExpiryTime())) {
            throw new ApiException("OTP has expired. Please request a new one.", HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(request.getOtp().trim(), latestOtp.getOtpHash())) {
            throw new ApiException("Invalid OTP.", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark all active OTPs for this user as used
        List<PasswordResetOtp> otps = otpRepository.findByEmailOrderByCreatedAtDesc(email);
        for (PasswordResetOtp otp : otps) {
            if (!otp.isUsed()) {
                otp.setUsed(true);
                otpRepository.save(otp);
            }
        }
    }
}
