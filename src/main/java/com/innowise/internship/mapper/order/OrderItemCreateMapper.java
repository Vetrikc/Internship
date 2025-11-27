package com.innowise.internship.mapper.order;

import com.innowise.internship.dto.order.CreateOrderItemDto;
import com.innowise.internship.entity.OrderItem;
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