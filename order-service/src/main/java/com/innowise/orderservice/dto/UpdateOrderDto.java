package com.innowise.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateOrderDto {
    private Long userId;
    private String status;
    private List<CreateOrderItemDto> orderItems;
}