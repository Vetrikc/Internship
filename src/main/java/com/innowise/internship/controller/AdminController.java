package com.innowise.internship.controller;


import com.innowise.internship.dto.UserDto;
import com.innowise.internship.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    final private UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/user/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/user/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/user")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String surname) {
        Page<UserDto> users = userService.getAllUsers(pageable, name, surname);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}
