package com.innowise.internship.mapper.order;

import com.innowise.internship.dto.order.OrderDto;
import com.innowise.internship.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {
    Order toEntity(OrderDto dto);
    OrderDto toDto(Order entity);
}