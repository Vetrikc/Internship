package com.innowise.internship.controller;// Updated Auth Controller - Change username to email in DTOs and methods

import com.innowise.internship.dao_auth.AuthUserRepository;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.AuthRequest;
import com.innowise.internship.dto.RefreshRequest;
import com.innowise.internship.dto.RegisterRequest;
import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.entity.Role;
import com.innowise.internship.entity.User;
import com.innowise.internship.exception.UnauthorizedException;
import com.innowise.internship.exception.UserNotFoundException;
import com.innowise.internship.security.JwtUtils;
import com.innowise.internship.service.CustomUserDetailsService;
import com.innowise.internship.service.UserService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          UserRepository userRepository, AuthUserRepository authUserRepository,
                          CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                          UserService userService, CustomUserDetailsService customUserDetailsService, UserRepository userRepository1) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
        this.userRepository = userRepository1;
    }

    // Login endpoint - Returns access and refresh tokens
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            AuthUser authUser = authUserRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User profile not found"));

            String accessToken = jwtUtils.generateAccessToken(authUser);
            String refreshToken = jwtUtils.generateRefreshToken(authUser);

            return ResponseEntity.ok(Map.of("accessToken", accessToken, "refreshToken", refreshToken));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверный email или пароль"));
        }
    }

    // Register endpoint - Save user credentials (admin can create users, or open for signup)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email уже зарегистрирован"));
        }

        // 1. Создаём учётные данные в отдельной БД
        AuthUser authUser = new AuthUser();
        authUser.setEmail(request.getEmail());
        authUser.setPassword(passwordEncoder.encode(request.getPassword()));
        authUser.setRole(Role.USER);//todo HARDCODED
        authUser.setActive(true);
        customUserDetailsService.createAuthUser(authUser);

        // 2. Создаём основной профиль пользователя
        UserDto userDto = new UserDto();
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

            AuthUser authUser = authUserRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

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

    @GetMapping("/me")
    public UserDto getCurrentUserInfo(Authentication authentication) throws UnauthorizedException {
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("Пользователь не авторизован");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setBirthDate(user.getBirthDate());

        return dto;
    }
}


