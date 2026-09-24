package com.maintainsoft.dto;

import jakarta.validation.constraints.Positive;

public record StockQuantityRequest(
        @Positive int quantity
) {
}
