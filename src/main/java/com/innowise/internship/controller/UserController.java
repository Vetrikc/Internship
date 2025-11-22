package com.innowise.internship.controller;

import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.exception.UnauthorizedException;
import com.innowise.internship.exception.UserNotFoundException;
import com.innowise.internship.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Пользователи", description = "Управление пользователями")
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping()
    public ResponseEntity<UserDto> updateUser( @Valid @RequestBody UserDto userDto,Authentication authentication) {
        AuthUser userDetails = (AuthUser) authentication.getPrincipal();
        Long userId = userDetails.getId();
        UserDto updatedUser = userService.updateUser(userId, userDto);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo(Authentication authentication) throws UnauthorizedException {
        AuthUser userDetails = (AuthUser) authentication.getPrincipal();
        return new ResponseEntity<>(userService.getUserById(userDetails.getId()).orElseThrow(() -> new UsernameNotFoundException("User not found")),
                HttpStatus.OK);
    }

}