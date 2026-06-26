package com.maintainsoft.dto;

import java.time.Instant;

public record DeleteResponse(
        String status,
        Instant timestamp
) {
}
