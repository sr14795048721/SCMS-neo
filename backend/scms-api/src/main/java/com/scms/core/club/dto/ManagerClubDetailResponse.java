package com.scms.core.club.dto;

import java.time.Instant;

public record ManagerClubDetailResponse(
        Long clubId,
        String clubName,
        String clubType,
        String description,
        String status,
        long memberCount,
        long pendingJoinRequestCount,
        long totalJoinRequestCount,
        long activityCount,
        Instant createdAt,
        Instant updatedAt
) {
}
