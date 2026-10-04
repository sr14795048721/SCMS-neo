package com.scms.core.score.dto;

public record ScoreSummaryClubResponse(
        Long clubId,
        String clubName,
        long score
) {
}
