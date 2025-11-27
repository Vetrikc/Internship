package com.innowise.internship.mapper.order;

import com.innowise.internship.dto.order.OrderItemDto;
import com.innowise.internship.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderItemMapper {
    OrderItem toEntity(OrderItemDto dto);
    OrderItemDto toDto(OrderItem entity);
}