package com.maintainsoft.dto;

import java.util.UUID;

public record MachineStatusResponse(
        UUID id,
        String name,
        String color,
        boolean builtIn
) {
}
