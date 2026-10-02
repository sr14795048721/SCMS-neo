package com.scms.core.score.dto;

import java.time.Instant;

public record ScoreRuleResponse(
        Long id,
        String name,
        int scoreDelta,
        String scopeType,
        Long clubId,
        String clubName,
        String status,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
