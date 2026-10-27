package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {
    Order toEntity(OrderDto dto);
    OrderDto toDto(Order entity);
}