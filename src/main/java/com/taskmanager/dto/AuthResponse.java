package com.taskmanager.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
    String token,
    String username,
    String role
) {}
