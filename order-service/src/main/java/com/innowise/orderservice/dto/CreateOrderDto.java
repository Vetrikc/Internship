package com.innowise.orderservice.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderDto {
    private Long userId;
    @NotBlank(message = "Status is required")
    private String status;
    @NotEmpty(message = "Order items cannot be empty")
    private List<CreateOrderItemDto> orderItems;
}