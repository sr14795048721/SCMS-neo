package com.scms.core.reward.dto;

import java.time.Instant;

public record StudentRewardItemResponse(
        Long id,
        String name,
        int scoreCost,
        int stock,
        String status,
        boolean hasImage,
        Instant createdAt,
        Instant updatedAt
) {
}
