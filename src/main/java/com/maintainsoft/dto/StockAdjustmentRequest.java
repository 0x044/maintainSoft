package com.maintainsoft.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record StockAdjustmentRequest(
        @PositiveOrZero int quantity
) {
}
