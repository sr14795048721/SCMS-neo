package com.scms.core.activity.dto;

import java.time.Instant;

public record AdminActivityRegistrationResponse(
        Long registrationId,
        Long userId,
        String username,
        String displayName,
        String studentNo,
        String grade,
        String className,
        Instant registeredAt
) {
}
