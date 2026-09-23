package com.maintainsoft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String phone,
        @NotNull UUID department
) {
}
