package com.maintainsoft.dto;

import java.time.Instant;
import java.util.UUID;

public record UpdateResponse(
        String status,
        Instant timeStamp,
        UUID uuid
) {
}
