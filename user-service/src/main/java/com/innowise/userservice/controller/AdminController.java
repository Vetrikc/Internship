package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    final private UserService userService;
    final private PaymentCardService paymentCardService;

    public AdminController(UserService userService, PaymentCardService paymentCardService) {
        this.userService = userService;
        this.paymentCardService = paymentCardService;
    }

    @GetMapping("/user")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String surname) {
        Page<UserDto> users = userService.getAllUsers(pageable, name, surname);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/user/{id}")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает данные пользователя")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(userDto -> new ResponseEntity<>(userDto, HttpStatus.OK))
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(@PathVariable Long id) {
        return  new ResponseEntity<> (paymentCardService.getCardById(id), HttpStatus.OK);
    }

    @GetMapping("/cards")
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(Pageable pageable) {
        Page<PaymentCardDto> cards = paymentCardService.getAllCards(pageable);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @GetMapping("/cards/users/{userId}")
    public ResponseEntity<List<PaymentCardDto>> getCardsByUserId(@PathVariable Long userId) {
        List<PaymentCardDto> cards = paymentCardService.getCardsByUserId(userId);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(@PathVariable Long id, @Valid @RequestBody PaymentCardDto cardDto) {
        PaymentCardDto updatedCard = paymentCardService.updateCard(id, cardDto);
        return new ResponseEntity<>(updatedCard, HttpStatus.OK);
    }

    @PutMapping("/cards/{id}/activate")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        paymentCardService.activateCard(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/cards/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(@PathVariable Long id) {
        paymentCardService.deactivateCard(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
