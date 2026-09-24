package com.maintainsoft.dto;

import com.maintainsoft.enums.RepairCostCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RepairCostResponse(
        UUID id,
        UUID repairId,
        RepairCostCategory category,
        BigDecimal amount,
        String currency,
        String description,
        Instant createdAt,
        String createdBy
) {
}
