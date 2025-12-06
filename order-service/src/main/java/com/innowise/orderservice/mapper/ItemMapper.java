package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    Item toEntity(ItemDto dto);
    ItemDto toDto(Item entity);
}