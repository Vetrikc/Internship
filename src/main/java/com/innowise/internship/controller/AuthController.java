package com.innowise.internship.controller;// Updated Auth Controller - Change username to email in DTOs and methods

import com.innowise.internship.dto.security.AuthRequest;
import com.innowise.internship.dto.security.RefreshRequest;
import com.innowise.internship.dto.security.RegisterRequest;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.entity.Role;
import com.innowise.internship.security.JwtUtils;
import com.innowise.internship.service.CustomUserDetailsService;
import com.innowise.internship.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          PasswordEncoder passwordEncoder,
                          UserService userService, CustomUserDetailsService customUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            AuthUser authUser = (AuthUser) customUserDetailsService.loadUserByUsername(request.getEmail());

            String accessToken = jwtUtils.generateAccessToken(authUser);
            String refreshToken = jwtUtils.generateRefreshToken(authUser);

            return ResponseEntity.ok(Map.of("accessToken", accessToken, "refreshToken", refreshToken));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверный email или пароль"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (customUserDetailsService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email уже зарегистрирован"));
        }

        // 1. Создаём учётные данные в отдельной БД//todo HARDCODED
        AuthUser authUser = new AuthUser();
        authUser.setEmail(request.getEmail());
        authUser.setPassword(passwordEncoder.encode(request.getPassword()));
        authUser.setRole(Role.USER);
        authUser.setActive(true);
        authUser = customUserDetailsService.createAuthUser(authUser);

        // 2. Создаём основной профиль пользователя
        UserDto userDto = new UserDto();
        userDto.setId(authUser.getId());
        userDto.setEmail(request.getEmail());
        userDto.setName(request.getName());
        userDto.setSurname(request.getSurname());
        userDto.setBirthDate(request.getBirthDate());

        userService.createUser(userDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User has been created"));
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

            return ResponseEntity.ok (Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }

}


