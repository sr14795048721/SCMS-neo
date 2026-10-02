package com.scms.core.reward.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentRewardOrderCreateRequest(
        @NotNull
        @Positive
        Long rewardId
) {
}
