package com.maintainsoft.dto;

import com.maintainsoft.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        Role role,
        UUID departmentId
) {
}
