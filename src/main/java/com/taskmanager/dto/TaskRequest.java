package com.taskmanager.dto;

import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record TaskRequest(
    @NotBlank(message = "Title is required")
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    Long assigneeId
) {}
