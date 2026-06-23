package com.maintainsoft.dto;

public record DepartmentRequest(
        String deptName,
        String pocName,
        Long pocNumber
) {
}
