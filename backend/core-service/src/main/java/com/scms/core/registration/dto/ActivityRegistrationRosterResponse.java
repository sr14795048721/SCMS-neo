package com.scms.core.registration.dto;

import java.time.Instant;

public record ActivityRegistrationRosterResponse(
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
