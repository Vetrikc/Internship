package com.innowise.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderWithUserDto {
    OrderDto order;
    UserDto user;
}