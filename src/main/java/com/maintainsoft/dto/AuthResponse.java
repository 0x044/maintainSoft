package com.maintainsoft.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role
) {
}
