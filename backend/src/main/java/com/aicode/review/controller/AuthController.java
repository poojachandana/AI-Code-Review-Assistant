package com.aicode.review.controller;

import com.aicode.review.dto.*;
import com.aicode.review.entity.User;
import com.aicode.review.security.JwtUtil;
import com.aicode.review.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Stateless JWT: logout is handled client-side by discarding the token. */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out. Discard the token on the client."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("email"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(AuthResponse.builder()
                .userId(user.getId()).name(user.getName()).email(user.getEmail()).role(user.getRole()).build());
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestHeader("Authorization") String authHeader,
                                                               @RequestBody UpdateProfileRequest request) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        authService.updateProfile(userId, request);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }
}
