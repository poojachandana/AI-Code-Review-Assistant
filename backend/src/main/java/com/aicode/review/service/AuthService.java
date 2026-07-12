package com.aicode.review.service;

import com.aicode.review.dto.*;
import com.aicode.review.entity.User;
import com.aicode.review.exception.BadRequestException;
import com.aicode.review.exception.ResourceNotFoundException;
import com.aicode.review.repository.UserRepository;
import com.aicode.review.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${app.admin.emails:}")
    private String adminEmailsRaw;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        String email = request.getEmail().toLowerCase();
        String role = shouldBeAdmin(email) ? "ROLE_ADMIN" : "ROLE_USER";

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .emailNotifications(true)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return toAuthResponse(user, token);
    }

    /** First user ever registered, or any email listed in app.admin.emails, becomes an admin. */
    private boolean shouldBeAdmin(String email) {
        if (userRepository.count() == 0) return true;
        List<String> adminEmails = Arrays.stream(adminEmailsRaw.split(","))
                .map(String::trim).map(String::toLowerCase).filter(s -> !s.isEmpty()).toList();
        return adminEmails.contains(email);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return toAuthResponse(user, token);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return userRepository.save(user);
    }

    /** Simple reset-password flow: in production this would email a reset token. */
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}

