package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.UserLookupDto;
import com.buffturf.buffturf_backend.exception.ApiException;
import com.buffturf.buffturf_backend.model.User;
import com.buffturf.buffturf_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Looks up an existing Buffturf user by email to add them as a friend/teammate during booking.
     * Only registered and active Buffturf users can be added.
     */
    @GetMapping("/lookup")
    public ResponseEntity<UserLookupDto> lookupUserByEmail(
            @RequestParam("email") String email,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (email == null || email.trim().isEmpty()) {
            throw new ApiException("Email address is required", HttpStatus.BAD_REQUEST);
        }

        String cleanEmail = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new ApiException("Invalid email format. Please enter a valid email address.", HttpStatus.BAD_REQUEST);
        }

        // Prevent captain from looking up or adding themselves
        if (userDetails != null && userDetails.getUsername() != null) {
            String currentAuth = userDetails.getUsername().trim().toLowerCase();
            if (currentAuth.equalsIgnoreCase(cleanEmail)) {
                throw new ApiException("You are the Team Captain and cannot add yourself as a squad friend.", HttpStatus.BAD_REQUEST);
            }
        }

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ApiException(
                        "No Buffturf account found for '" + cleanEmail + "'. Friends must be registered on Buffturf to be added to the squad.",
                        HttpStatus.NOT_FOUND
                ));

        if (user.isBanned()) {
            throw new ApiException("This player's account is suspended and cannot be added to match bookings.", HttpStatus.BAD_REQUEST);
        }

        UserLookupDto dto = new UserLookupDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole() != null ? user.getRole().name() : "USER",
                true
        );

        return ResponseEntity.ok(dto);
    }
}
