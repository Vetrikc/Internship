package com.innowise.internship.controller;

import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.exception.CardNotFoundException;
import com.innowise.internship.service.PaymentCardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    public PaymentCardController(PaymentCardService paymentCardService) {
        this.paymentCardService = paymentCardService;
    }

    @PostMapping
    public ResponseEntity<PaymentCardDto> createCard(@Valid @RequestBody PaymentCardDto cardDto) {
        PaymentCardDto createdCard = paymentCardService.createCard(cardDto);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(@PathVariable Long id) {
        return paymentCardService.getCardById(id)
                .map(cardDto -> new ResponseEntity<>(cardDto, HttpStatus.OK))
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(Pageable pageable) {
        Page<PaymentCardDto> cards = paymentCardService.getAllCards(pageable);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<PaymentCardDto>> getCardsByUserId(@PathVariable Long userId) {
        List<PaymentCardDto> cards = paymentCardService.getCardsByUserId(userId);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(@PathVariable Long id, @Valid @RequestBody PaymentCardDto cardDto) {
        PaymentCardDto updatedCard = paymentCardService.updateCard(id, cardDto);
        return new ResponseEntity<>(updatedCard, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        paymentCardService.deleteCard(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        paymentCardService.activateCard(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(@PathVariable Long id) {
        paymentCardService.deactivateCard(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}