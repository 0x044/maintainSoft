package com.maintainsoft.dto;

import java.util.UUID;

public record AssignRepairRequest(
        UUID assignedSupervisorId
) {
}
