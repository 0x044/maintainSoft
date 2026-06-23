package com.maintainsoft.dto;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timeStamp
) {
}
