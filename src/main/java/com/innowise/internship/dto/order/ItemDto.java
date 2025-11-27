package com.innowise.internship.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ItemDto {
    private Long id;
    @NotBlank
    private String name;
    @Positive
    private BigDecimal price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}