package com.cirs.service.impl;

import com.cirs.dto.request.LoginRequest;
import com.cirs.dto.request.RegisterRequest;
import com.cirs.dto.response.AuthResponse;
import com.cirs.dto.response.UserProfileResponse;
import com.cirs.entity.User;
import com.cirs.enums.Role;
import com.cirs.exception.BadRequestException;
import com.cirs.exception.ResourceNotFoundException;
import com.cirs.exception.UnauthorizedAccessException;
import com.cirs.repository.UserRepository;
import com.cirs.security.CustomUserDetails;
import com.cirs.security.JwtTokenProvider;
import com.cirs.service.AuditService;
import com.cirs.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new BadRequestException("Username is already taken. Please choose another.");
        }

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email is already registered. Please login or use another email.");
        }

        User user = new User(
                request.getUsername().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null,
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);

        auditService.log(savedUser, "USER_REGISTRATION", "USER", savedUser.getId().toString(),
                "New user registered with username: " + savedUser.getUsername(), httpRequest);

        // Authenticate the newly registered user and return token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        return new AuthResponse(token, savedUser.getId(), savedUser.getUsername(), savedUser.getFullName(), savedUser.getRole());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.getUsername().trim();

        // Support login by email or username
        String username = identifier;
        if (identifier.contains("@")) {
            username = userRepository.findByEmail(identifier.toLowerCase())
                    .map(User::getUsername)
                    .orElse(identifier);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!user.isActive()) {
                throw new UnauthorizedAccessException("Account is disabled. Please contact the administrator.");
            }

            String token = tokenProvider.generateToken(authentication);

            auditService.log(user, "LOGIN_SUCCESS", "USER", user.getId().toString(),
                    "User logged in successfully", httpRequest);

            return new AuthResponse(token, user.getId(), user.getUsername(), user.getFullName(), user.getRole());

        } catch (BadCredentialsException ex) {
            auditService.log(null, "LOGIN_FAILED", "AUTH", identifier,
                    "Failed login attempt for identifier: " + identifier, httpRequest);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = getUserByUsername(username);
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
