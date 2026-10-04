package com.scms.core.attendance.dto;

import java.time.Instant;

public record PublicAttendanceSignResponse(
        String displayName,
        String grade,
        String className,
        String role,
        Instant checkInAt
) {
}
