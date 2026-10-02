package com.scms.core.club.dto;

import java.time.Instant;

public record ManagerClubSummaryResponse(
        Long clubId,
        String clubName,
        String clubType,
        String description,
        long memberCount,
        long pendingJoinRequestCount,
        long draftActivityCount,
        Instant createdAt
) {
}
