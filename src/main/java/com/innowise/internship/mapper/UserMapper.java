package com.innowise.internship.mapper;

import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entitiy.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "paymentCards", ignore = true)
    User toEntity(UserDto userDto);
}