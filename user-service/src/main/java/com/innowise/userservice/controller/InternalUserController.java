package com.innowise.userservice.controller;

import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.dto.UserProfileCreateRequest;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUserProfile(@RequestBody UserProfileCreateRequest request) {
        UserDto dto = new UserDto();
        dto.setId(request.getId());
        dto.setEmail(request.getEmail());
        dto.setName(request.getName());
        dto.setSurname(request.getSurname());
        dto.setBirthDate(request.getBirthDate());

        userService.createUser(dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
    @GetMapping("/by-email/{email}")
    public UserDto getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email).get();
    }
}
