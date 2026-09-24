package com.maintainsoft.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StockIssueRequest(
        @Positive int quantity,
        @NotNull UUID repairId
) {
}
