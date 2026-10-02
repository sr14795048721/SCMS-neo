package com.scms.core.club.dto;

import java.time.Instant;

public record StudentPendingClubJoinRequestResponse(
        Long requestId,
        Long clubId,
        String clubName,
        String clubType,
        String description,
        long memberCount,
        String reason,
        String status,
        Instant createdAt
) {
}
