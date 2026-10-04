package com.scms.core.club.dto;

import java.time.Instant;

public record StudentDiscoverClubResponse(
        Long clubId,
        String clubName,
        String clubType,
        String description,
        long memberCount,
        String memberRole,
        Long dutyId,
        String dutyName,
        java.util.List<String> dutyPermissions,
        boolean canManage,
        Instant createdAt
) {
}
