package com.scms.core.score.dto;

public record ClubScoreRankingResponse(
        int rank,
        Long userId,
        String username,
        String displayName,
        String studentNo,
        long score
) {
}
