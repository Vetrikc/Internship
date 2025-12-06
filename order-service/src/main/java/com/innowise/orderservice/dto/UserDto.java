package com.innowise.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserDto{
    Long id;
    String email;
    String name;
    String surname;
    LocalDate birthDate;
}