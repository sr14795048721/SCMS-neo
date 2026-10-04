package com.scms.core.attendance.dto;

import jakarta.validation.constraints.NotNull;

public record ManagerAttendanceMarkRequest(
        @NotNull(message = "student user id is required")
        Long studentUserId
) {
}
