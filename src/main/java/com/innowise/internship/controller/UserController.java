package com.innowise.internship.controller;

import com.innowise.internship.dto.UserDto;
import com.innowise.internship.exception.UserNotFoundException;
import com.innowise.internship.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto createdUser = userService.createUser(userDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(userDto -> new ResponseEntity<>(userDto, HttpStatus.OK))
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String surname) {
        Page<UserDto> users = userService.getAllUsers(pageable, name, surname);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(id, userDto);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}