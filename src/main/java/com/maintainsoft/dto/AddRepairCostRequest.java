package com.maintainsoft.dto;

import com.maintainsoft.enums.RepairCostCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddRepairCostRequest(
        @NotNull RepairCostCategory category,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Size(max = 1000) String description
) {
}
