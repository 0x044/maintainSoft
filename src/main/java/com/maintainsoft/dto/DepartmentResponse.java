package com.maintainsoft.dto;

import java.util.UUID;

public record DepartmentResponse(
        UUID deptId,
        String name,
        String pocName,
        Long pocPhone
) {
}
