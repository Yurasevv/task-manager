package com.taskmanager.dto;

import lombok.Builder;

@Builder
public record UserResponse(
    Long id,
    String username,
    String email,
    String role
) {}
