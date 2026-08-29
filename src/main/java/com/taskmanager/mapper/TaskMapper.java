package com.taskmanager.mapper;

import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class})
public interface TaskMapper {

    TaskResponse toResponse(Task task);

}
