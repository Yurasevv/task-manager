package com.taskmanager.mapper;

import com.taskmanager.dto.UserResponse;
import com.taskmanager.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    UserResponse toResponse(User user);

}
