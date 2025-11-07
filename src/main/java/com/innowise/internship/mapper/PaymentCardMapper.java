package com.innowise.internship.mapper;

import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.entitiy.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "userId", source = "user.id")
    PaymentCardDto toDto(PaymentCard paymentCard);

    @Mapping(target = "user", ignore = true)
    PaymentCard toEntity(PaymentCardDto paymentCardDto);
}