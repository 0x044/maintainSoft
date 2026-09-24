package com.maintainsoft.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StockReturnRequest(
        @Positive int quantity,
        @NotNull UUID repairId
) {
}
