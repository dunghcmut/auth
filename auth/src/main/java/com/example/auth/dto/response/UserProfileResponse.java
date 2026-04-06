package com.example.auth.dto.response;

import com.example.auth.entity.Role;
import com.example.auth.entity.UserStatus;

import java.time.LocalDateTime;

public record UserProfileResponse(
    Long id,
    String name,
    String email,
    Role role,
    UserStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
