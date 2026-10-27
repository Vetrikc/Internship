package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.security.JwtPrincipal;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@PreAuthorize("hasRole('USER')")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    public PaymentCardController(PaymentCardService paymentCardService) {
        this.paymentCardService = paymentCardService;
    }

    @GetMapping()
    public ResponseEntity<List<PaymentCardDto>> getAllUserCards(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        List<PaymentCardDto> cards =
                paymentCardService.getCardsByUserId(userId);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<PaymentCardDto> createCard(@Valid @RequestBody PaymentCardDto cardDto, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        PaymentCardDto createdCard = paymentCardService.createCard(cardDto);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(@PathVariable Long id, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        PaymentCardDto cardDto = paymentCardService.getUserCardById(id,userId);
        return new ResponseEntity<>(cardDto, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(@PathVariable Long id, @Valid @RequestBody PaymentCardDto cardDto, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();


        PaymentCardDto updatedCard = paymentCardService.updateUserCard(id, cardDto, userId);
        return new ResponseEntity<>(updatedCard, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.id();

        paymentCardService.deleteUserCard(id, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}