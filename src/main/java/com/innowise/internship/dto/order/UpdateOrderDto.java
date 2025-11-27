package com.innowise.internship.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UpdateOrderDto {
    private String userEmail;
    private String status;
    private List<CreateOrderItemDto> orderItems;
}