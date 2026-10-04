package com.scms.core.attendance.dto;

import java.time.Instant;

public record PublicAttendanceSessionResponse(
        String clubName,
        String title,
        String status,
        String scoreRuleName,
        int scoreDelta,
        Instant startedAt
) {
}
