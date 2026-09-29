package com.cirs.controller;

import com.cirs.dto.request.LoginRequest;
import com.cirs.dto.request.RegisterRequest;
import com.cirs.dto.response.ApiResponse;
import com.cirs.dto.response.AuthResponse;
import com.cirs.dto.response.UserProfileResponse;
import com.cirs.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                             HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                          HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(ApiResponse.error("Not authenticated"), HttpStatus.UNAUTHORIZED);
        }
        UserProfileResponse profile = authService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
