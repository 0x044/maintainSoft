package com.maintainsoft.dto;

import jakarta.validation.constraints.Size;

public record UpdateSpareRequest(
        @Size(max = 255) String partNumber,
        @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @Size(max = 32) String unit,
        @Size(max = 255) String compatibleMachine
) {
}
