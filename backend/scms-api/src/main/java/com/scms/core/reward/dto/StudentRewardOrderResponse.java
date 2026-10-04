package com.scms.core.reward.dto;

import java.time.Instant;

public record StudentRewardOrderResponse(
        Long id,
        Long rewardId,
        String rewardName,
        int scoreCost,
        String status,
        Instant createdAt,
        Instant completedAt,
        Instant rejectedAt
) {
}
