package com.innowise.internship.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentCardDto {
    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 19, message = "Card number must be between 16 and 19 characters")
    private String number;

    @NotBlank(message = "Holder name is required")
    private String holder;

    @FutureOrPresent(message = "Expiration date must be in the future or present")
    private LocalDate expirationDate;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}