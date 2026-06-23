package com.maintainsoft.dto;

import java.util.UUID;

public record RegisterRequest(
        String name,
        String email,
        String password,
        String phone,
        UUID department
) {
}
