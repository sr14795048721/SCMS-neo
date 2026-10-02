package com.scms.core.report.infra;

public record AdminClubRankingRow(
        long clubId,
        String clubName,
        long memberCount,
        long activityCount
) {
}
