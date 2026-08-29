package com.taskmanager.dto;

import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    UserResponse author,
    UserResponse assignee,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
