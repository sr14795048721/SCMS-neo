package com.scms.core.registration.dto;

import java.time.Instant;

public record RegistrationResponse(
        Long id,
        Long activityId,
        Long userId,
        String status,
        Instant createdAt,
        Instant canceledAt
) {
}
