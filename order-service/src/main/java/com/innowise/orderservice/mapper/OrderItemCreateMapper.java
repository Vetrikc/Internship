package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.CreateOrderItemDto;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemCreateMapper {
    @Mapping(target = "item.id", source = "itemId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderItem toEntity(CreateOrderItemDto dto);
}