package com.maintainsoft.dto;

import com.maintainsoft.enums.RepairPriority;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateRepairRequest(
        @Size(max = 1000) String description,
        RepairPriority repairPriority,
        @Size(max = 255) String externalTechnicianName,
        @Size(max = 32) String externalTechnicianPhone,
        Instant startDate
) {
}
