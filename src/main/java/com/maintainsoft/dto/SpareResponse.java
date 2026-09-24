package com.maintainsoft.dto;

import java.util.UUID;

public record SpareResponse(
        UUID id,
        String partNumber,
        String name,
        String description,
        String unit,
        String compatibleMachine,
        int stock
) {
}
