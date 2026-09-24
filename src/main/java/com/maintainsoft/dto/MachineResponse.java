package com.maintainsoft.dto;

import java.time.Instant;
import java.util.UUID;

public record MachineResponse(
        UUID id,
        String name,
        String serialNumber,
        UUID departmentId,
        UUID statusId,
        String statusName,
        String statusColor,
        boolean statusBuiltIn,
        String equipmentType,
        String manufacturer,
        String model,
        String location,
        Instant commissionedAt,
        Instant decommissionedAt,
        Instant lastServicedAt,
        Instant nextServiceDueAt,
        Integer maintenanceIntervalDays
) {
}
