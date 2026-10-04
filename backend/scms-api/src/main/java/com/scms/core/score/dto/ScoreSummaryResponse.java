package com.scms.core.score.dto;

import java.util.List;

public record ScoreSummaryResponse(
        long totalScore,
        long redeemedScore,
        long balanceScore,
        List<ScoreSummaryClubResponse> clubs
) {
}
