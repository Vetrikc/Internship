package com.innowise.internship.controller;

import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.entity.AuthUser;
import com.innowise.internship.service.PaymentCardService;
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
        List<PaymentCardDto> cards =
                paymentCardService.getCardsByUserId(((AuthUser) authentication.getPrincipal()).getId());
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<PaymentCardDto> createCard(@Valid @RequestBody PaymentCardDto cardDto, Authentication authentication) {
        AuthUser userDetails = (AuthUser) authentication.getPrincipal();
        cardDto.setUserId(userDetails.getId());
        PaymentCardDto createdCard = paymentCardService.createCard(cardDto);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(@PathVariable Long id, Authentication authentication) {
        PaymentCardDto cardDto = paymentCardService.getUserCardById(id, ((AuthUser) authentication.getPrincipal()).getId());
        return new ResponseEntity<>(cardDto, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(@PathVariable Long id, @Valid @RequestBody PaymentCardDto cardDto, Authentication authentication) {
        PaymentCardDto updatedCard = paymentCardService.updateUserCard(id, cardDto, ((AuthUser) authentication.getPrincipal()).getId());
        return new ResponseEntity<>(updatedCard, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id, Authentication authentication) {
        paymentCardService.deleteUserCard(id, ((AuthUser) authentication.getPrincipal()).getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}