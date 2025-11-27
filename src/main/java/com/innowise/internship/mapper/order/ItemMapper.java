package com.innowise.internship.mapper.order;

import com.innowise.internship.dto.order.ItemDto;
import com.innowise.internship.entity.Item;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    Item toEntity(ItemDto dto);
    ItemDto toDto(Item entity);
}