package com.scms.core.reward.dto;

import java.time.Instant;

public record AdminRewardOrderResponse(
        Long id,
        Long rewardId,
        String rewardName,
        Long userId,
        String username,
        String displayName,
        String studentNo,
        int scoreCost,
        String status,
        Instant createdAt,
        Instant completedAt,
        Long completedBy,
        String completedByName,
        Instant rejectedAt,
        Long rejectedBy,
        String rejectedByName
) {
}
