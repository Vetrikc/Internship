package com.innowise.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderItemDto {
    @NotNull(message = "Item ID is required")
    @Positive(message = "Item ID must be positive")
    private Long itemId;
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}