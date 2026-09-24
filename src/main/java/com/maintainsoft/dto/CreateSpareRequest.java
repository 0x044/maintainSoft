package com.maintainsoft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateSpareRequest(
        @NotBlank @Size(max = 255) String partNumber,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @Size(max = 32) String unit,
        @Size(max = 255) String compatibleMachine,
        @PositiveOrZero int initialStock
) {
}
