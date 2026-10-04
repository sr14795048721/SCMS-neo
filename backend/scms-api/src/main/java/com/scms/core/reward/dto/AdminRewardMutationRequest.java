package com.scms.core.reward.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminRewardMutationRequest(
        @NotBlank(message = "reward name is required")
        String name,
        @NotNull(message = "score cost is required")
        @Min(value = 0, message = "score cost must be greater than or equal to 0")
        Integer scoreCost,
        @NotNull(message = "stock is required")
        @Min(value = 0, message = "stock must be greater than or equal to 0")
        Integer stock,
        @NotBlank(message = "reward visibility scope is required")
        String visibilityScope,
        List<Long> clubIds,
        @NotBlank(message = "status is required")
        String status
) {
}
