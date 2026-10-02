package com.scms.core.reward.dto;

import java.time.Instant;
import java.util.List;

public record AdminRewardItemResponse(
        Long id,
        String name,
        int scoreCost,
        int stock,
        String status,
        String visibilityScope,
        List<Long> clubIds,
        List<String> clubNames,
        boolean hasImage,
        Instant createdAt,
        Instant updatedAt
) {
}
