package com.maintainsoft.dto;

import com.maintainsoft.entity.Department;

public record RegisterRequest(
        String name,
        String email,
        String password,
        String phone,
        Department department
) {
}
