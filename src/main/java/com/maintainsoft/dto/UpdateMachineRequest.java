package com.maintainsoft.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateMachineRequest(
        @Size(max = 255) String name,
        @Size(max = 255) String serialNumber,
        UUID departmentId,
        UUID statusId,
        @Size(max = 255) String equipmentType,
        @Size(max = 255) String manufacturer,
        @Size(max = 255) String model,
        @Size(max = 255) String location,
        Instant commissionedAt,
        Instant decommissionedAt,
        Instant lastServicedAt,
        Instant nextServiceDueAt,
        @Positive Integer maintenanceIntervalDays
) {
}
