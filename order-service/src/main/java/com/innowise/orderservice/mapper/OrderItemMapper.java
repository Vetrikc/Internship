package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderItemDto;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderItemMapper {
    OrderItem toEntity(OrderItemDto dto);
    OrderItemDto toDto(OrderItem entity);
}