package com.scms.core.report.dto;

public record AdminClubRankingResponse(
        long rank,
        long clubId,
        String clubName,
        long memberCount,
        long activityCount
) {
}
