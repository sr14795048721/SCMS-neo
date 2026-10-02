package com.scms.core.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManagerAttendanceSessionCreateRequest(
        @NotBlank(message = "attendance session title is required")
        @Size(max = 120, message = "attendance session title must be at most 120 characters")
        String title,

        @NotNull(message = "score rule is required")
        Long scoreRuleId
) {
}
