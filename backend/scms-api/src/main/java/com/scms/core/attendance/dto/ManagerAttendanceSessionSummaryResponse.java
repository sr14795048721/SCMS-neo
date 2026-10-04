package com.scms.core.attendance.dto;

import java.time.Instant;

public record ManagerAttendanceSessionSummaryResponse(
        Long sessionId,
        Long clubId,
        String title,
        Long scoreRuleId,
        String scoreRuleName,
        int scoreDelta,
        String status,
        String shareToken,
        int totalMembers,
        int checkedInCount,
        int checkedOutCount,
        int settledCount,
        Instant startedAt,
        Instant endedAt,
        Instant settledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
