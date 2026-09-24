package com.maintainsoft.dto;

import com.maintainsoft.enums.RepairPriority;
import com.maintainsoft.enums.RepairStatus;
import com.maintainsoft.enums.RepairType;

import java.time.Instant;
import java.util.UUID;

public record RepairResponse(
        UUID id,
        UUID machineId,
        String machineName,
        RepairStatus status,
        RepairType type,
        RepairPriority priority,
        String description,
        Instant startDate,
        Instant endDate,
        UUID assignedSupervisorId,
        String assignedSupervisorName,
        String externalTechnicianName,
        String externalTechnicianPhone,
        String idempotencyKey
) {
}
