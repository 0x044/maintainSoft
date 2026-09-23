package com.maintainsoft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 255) String deptName,
        @NotBlank @Size(max = 255) String pocName,
        @NotNull @Positive Long pocNumber
) {
}
