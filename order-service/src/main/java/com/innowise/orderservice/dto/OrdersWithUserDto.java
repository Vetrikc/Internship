package com.innowise.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrdersWithUserDto {
    List<OrderDto> order;
    UserDto user;
}