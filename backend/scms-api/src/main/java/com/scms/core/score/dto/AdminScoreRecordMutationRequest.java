package com.scms.core.score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminScoreRecordMutationRequest(
        @NotNull(message = "clubId is required")
        Long clubId,
        @NotNull(message = "userId is required")
        Long userId,
        Long ruleId,
        @NotNull(message = "scoreDelta is required")
        Integer scoreDelta,
        @NotBlank(message = "reason is required")
        String reason
) {
}
