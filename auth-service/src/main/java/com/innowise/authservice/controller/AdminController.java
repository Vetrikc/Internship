package com.innowise.authservice.controller;



import com.innowise.authservice.service.CustomUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    final private CustomUserDetailsService customUserDetailsService;

    public AdminController(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @PutMapping("/user/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        customUserDetailsService.activateUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/user/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        customUserDetailsService.deactivateUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/user/{id}/admin")
    public ResponseEntity<Void> setAdmin(@PathVariable Long id) {
        customUserDetailsService.setAdminRole(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
