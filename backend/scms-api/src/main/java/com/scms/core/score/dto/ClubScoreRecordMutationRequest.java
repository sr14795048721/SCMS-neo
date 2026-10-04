package com.scms.core.score.dto;

import jakarta.validation.constraints.NotNull;

public record ClubScoreRecordMutationRequest(
        @NotNull(message = "userId is required")
        Long userId,
        Long ruleId,
        Integer scoreDelta,
        String reason
) {
}
