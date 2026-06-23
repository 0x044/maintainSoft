package com.maintainsoft.dto;

import com.maintainsoft.entity.Department;

import java.util.UUID;

public record RegisterRequest(
        String name,
        String email,
        String password,
        String phone,
        UUID department
) {
}
