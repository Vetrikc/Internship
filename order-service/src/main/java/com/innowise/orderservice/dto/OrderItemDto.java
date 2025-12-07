package com.innowise.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderItemDto {
    private Long id;
    private ItemDto item;
    private int quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}