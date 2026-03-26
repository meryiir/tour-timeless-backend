package com.tourisme.controller;

import com.tourisme.dto.request.GoogleAuthRequest;
import com.tourisme.dto.request.LoginRequest;
import com.tourisme.dto.request.RegisterRequest;
import com.tourisme.dto.response.AuthResponse;
import com.tourisme.dto.response.UserResponse;
import com.tourisme.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    @Value("${app.google.client-id:}")
    private String googleClientId;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.googleAuth(request.getToken());
        return ResponseEntity.ok(response);
    }

    /** Public: OAuth web client IDs are not secret; used when VITE_GOOGLE_CLIENT_ID is unset. */
    @GetMapping("/google-client-id")
    public ResponseEntity<Map<String, String>> getGoogleClientId() {
        String id = googleClientId != null ? googleClientId.trim() : "";
        return ResponseEntity.ok(Map.of("clientId", id));
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }
}
