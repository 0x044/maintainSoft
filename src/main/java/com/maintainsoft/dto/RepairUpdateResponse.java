package com.maintainsoft.dto;

import com.maintainsoft.enums.RepairStatus;

import java.time.Instant;
import java.util.UUID;

public record RepairUpdateResponse(
        UUID id,
        UUID repairId,
        RepairStatus status,
        String description,
        Instant createdAt,
        String createdBy
) {
}
