package com.maintainsoft.dto;

import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateRepairRequest(
        @NotNull UUID machineId,
        @NotNull RepairType repairType,
        RepairPriority repairPriority,
        @NotBlank @Size(max = 1000) String description,
        @NotBlank @Size(max = 255) String idempotencyKey,
        @Size(max = 255) String externalTechnicianName,
        @Size(max = 32) String externalTechnicianPhone,
        Instant startDate
) {
}
