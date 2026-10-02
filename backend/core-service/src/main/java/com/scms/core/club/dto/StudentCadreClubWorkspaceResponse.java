package com.scms.core.club.dto;

import java.time.Instant;
import java.util.List;

public record StudentCadreClubWorkspaceResponse(
        Long clubId,
        String clubName,
        String clubType,
        String description,
        String status,
        long memberCount,
        long pendingJoinRequestCount,
        long totalJoinRequestCount,
        long activityCount,
        Long dutyId,
        String dutyName,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
}
