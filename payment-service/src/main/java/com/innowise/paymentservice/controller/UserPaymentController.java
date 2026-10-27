package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.PaymentDTO;
import com.innowise.paymentservice.security.JwtPrincipal;
import com.innowise.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('USER')")
public class UserPaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@RequestBody PaymentDTO paymentDTO) {
        PaymentDTO createdPayment = paymentService.createPayment(paymentDTO);
        return new ResponseEntity<>(createdPayment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> findPayments(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status) {
        Long userId = principal.id();
        List<PaymentDTO> payments = paymentService.findByUserIdOrOrderIdOrStatus(userId, orderId, status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/total/user/{userId}")
    public ResponseEntity<BigDecimal> getTotalSumForUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        Long userId = principal.id();
        BigDecimal total = paymentService.getTotalSumForUserInDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(total);
    }
}

