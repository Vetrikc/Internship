package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.PaymentDTO;
import com.innowise.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toEntity(PaymentDTO dto);

    PaymentDTO toDto(Payment entity);
}