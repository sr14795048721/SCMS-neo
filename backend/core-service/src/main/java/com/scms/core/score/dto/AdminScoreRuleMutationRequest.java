package com.scms.core.score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminScoreRuleMutationRequest(
        @NotBlank(message = "rule name is required")
        String name,
        @NotNull(message = "score delta is required")
        Integer scoreDelta,
        @NotBlank(message = "scope type is required")
        String scopeType,
        Long clubId,
        @NotBlank(message = "status is required")
        String status
) {
}
