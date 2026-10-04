package com.scms.core.club.dto;

import java.time.Instant;

public record ManagerClubJoinRequestResponse(
        Long requestId,
        Long clubId,
        Long studentUserId,
        String username,
        String displayName,
        String studentNo,
        String grade,
        String className,
        String reason,
        String status,
        Long reviewedBy,
        String reviewedByName,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
