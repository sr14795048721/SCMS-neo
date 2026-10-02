package com.scms.core.score.dto;

import java.time.Instant;

public record ClubScoreRecordResponse(
        Long id,
        Long userId,
        String username,
        String displayName,
        String studentNo,
        Long ruleId,
        String ruleName,
        int scoreDelta,
        String reason,
        Long operatorUserId,
        String operatorName,
        Instant createdAt
) {
}
