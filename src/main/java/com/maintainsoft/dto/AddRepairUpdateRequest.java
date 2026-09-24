package com.maintainsoft.dto;

import com.maintainsoft.enums.RepairStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddRepairUpdateRequest(
        @NotNull RepairStatus status,
        @NotBlank @Size(max = 2000) String description
) {
}
