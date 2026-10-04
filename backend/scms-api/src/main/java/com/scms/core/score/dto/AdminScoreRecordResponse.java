package com.scms.core.score.dto;

import java.time.Instant;

public record AdminScoreRecordResponse(
        Long id,
        Long clubId,
        String clubName,
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
