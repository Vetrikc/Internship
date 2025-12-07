package com.innowise.userservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
public class UserProfileCreateRequest {
    Long id;
    String email;
    String name;
    String surname;
    LocalDate birthDate;
}