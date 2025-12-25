package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.PaymentDTO;
import com.innowise.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> findPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status) {
        List<PaymentDTO> payments = paymentService.findByUserIdOrOrderIdOrStatus(userId, orderId, status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/total/all")
    public ResponseEntity<BigDecimal> getTotalSumForAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        BigDecimal total = paymentService.getTotalSumForAllInDateRange(startDate, endDate);
        return ResponseEntity.ok(total);
    }
}