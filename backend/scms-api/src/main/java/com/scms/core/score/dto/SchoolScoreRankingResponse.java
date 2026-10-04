package com.scms.core.score.dto;

public record SchoolScoreRankingResponse(
        int rank,
        Long clubId,
        String clubName,
        long memberCount,
        long totalScore,
        double avgScore
) {
}
