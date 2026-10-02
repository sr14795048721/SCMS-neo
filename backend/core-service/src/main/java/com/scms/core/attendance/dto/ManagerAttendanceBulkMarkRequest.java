package com.scms.core.attendance.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ManagerAttendanceBulkMarkRequest(
        @NotEmpty(message = "student user ids are required")
        List<@NotNull(message = "student user id is required") Long> studentUserIds
) {
}
