package com.scms.core.attendance.dto;

import java.time.Instant;

public record ManagerAttendanceSessionMemberResponse(
        Long studentUserId,
        String displayName,
        String grade,
        String className,
        String status,
        String signedName,
        String signedRole,
        String signaturePath,
        Instant checkInAt,
        Instant checkOutAt,
        boolean settled
) {
}
