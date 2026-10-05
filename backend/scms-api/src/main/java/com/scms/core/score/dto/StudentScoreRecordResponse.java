package com.scms.core.score.dto;

import java.time.Instant;

public record StudentScoreRecordResponse(
        Long id,
        Long clubId,
        String clubName,
        Long ruleId,
        String ruleName,
        int scoreDelta,
        String reason,
        String operatorName,
        Instant createdAt
) {
}
