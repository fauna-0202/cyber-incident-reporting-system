package com.cirs.service;

import com.cirs.dto.request.LoginRequest;
import com.cirs.dto.request.RegisterRequest;
import com.cirs.dto.response.AuthResponse;
import com.cirs.dto.response.UserProfileResponse;
import com.cirs.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    User getUserByUsername(String username);
    UserProfileResponse getUserProfile(String username);
}
