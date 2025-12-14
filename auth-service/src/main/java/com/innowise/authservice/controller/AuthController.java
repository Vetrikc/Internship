package com.innowise.authservice.controller;// Updated Auth Controller - Change username to email in DTOs and methods

import com.innowise.authservice.dto.*;
import com.innowise.authservice.entity.AuthUser;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.security.JwtUtils;
import com.innowise.authservice.service.CustomUserDetailsService;
import feign.FeignException;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@EnableFeignClients
@Log4j2
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserServiceClient userServiceClient;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          PasswordEncoder passwordEncoder,
                          CustomUserDetailsService customUserDetailsService, UserServiceClient userServiceClient) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.customUserDetailsService = customUserDetailsService;
        this.userServiceClient = userServiceClient;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            AuthUser authUser = (AuthUser) customUserDetailsService.loadUserByUsername(request.getEmail());

            String accessToken = jwtUtils.generateAccessToken(authUser);
            String refreshToken = jwtUtils.generateRefreshToken(authUser);

            return ResponseEntity.ok(Map.of("accessToken", accessToken, "refreshToken", refreshToken));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (customUserDetailsService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "The Email is already registered"));
        }

        // 1. Создаём учётные данные в отдельной БД
        AuthUser authUser = new AuthUser();
        authUser.setEmail(request.getEmail());
        authUser.setPassword(passwordEncoder.encode(request.getPassword()));
        authUser.setRole(Role.USER);//todo HARDCODED
        authUser.setActive(true);
        authUser = customUserDetailsService.createAuthUser(authUser);

        // 2. Создаём основной профиль пользователя
        UserProfileCreateRequest profileRequest = new UserProfileCreateRequest();
        profileRequest.setId(authUser.getId());
        profileRequest.setEmail(request.getEmail());
        profileRequest.setName(request.getName());
        profileRequest.setSurname(request.getSurname());
        profileRequest.setBirthDate(request.getBirthDate());

        try {
            log.info("Trying to call user-service at http://localhost:8082/api/v1/internal/users");
            userServiceClient.createUserProfile(profileRequest);
            log.info("User profile created successfully");
        } catch (FeignException e) {
            // Откатываем AuthUser, если user-service упал
            customUserDetailsService.deleteAuthUser(authUser.getId());
            log.error("Failed to call user-service: {} – {}", e.status(), e.getMessage());
            return ResponseEntity.status(502)
                    .body(Map.of("error", "Service temporarily unavailable"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }


    // Refresh token endpoint
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        try {
            String email = jwtUtils.extractEmail(refreshToken);

            AuthUser authUser = (AuthUser) customUserDetailsService.loadUserByUsername(email);

            if (!jwtUtils.validateToken(refreshToken, authUser)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired refresh token"));
            }

            String newAccessToken = jwtUtils.generateAccessToken(authUser);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody ValidateRequest request) {
        String token = request.getToken();

        try {
            String email = jwtUtils.extractEmail(token);

            AuthUser authUser = (AuthUser) customUserDetailsService.loadUserByUsername(email);

            boolean isValid = jwtUtils.validateToken(token, authUser);

            return ResponseEntity.ok().body(Map.of("valid", isValid, "email", email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }


}


