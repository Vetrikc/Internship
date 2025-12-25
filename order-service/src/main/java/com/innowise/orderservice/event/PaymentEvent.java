package com.innowise.orderservice.event;

import lombok.Data;

@Data
public class PaymentEvent {
    private Long orderId;
    private String status;  // SUCCESS или FAILED
}