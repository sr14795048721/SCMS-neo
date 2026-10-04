package com.scms.core.activity.dto;

import java.time.Instant;
import java.util.List;

public record AdminActivityDetailResponse(
        Long id,
        Long clubId,
        String clubName,
        String title,
        String description,
        String location,
        Instant startTime,
        Instant endTime,
        int capacity,
        String status,
        long registrationCount,
        Instant createdAt,
        Instant updatedAt,
        List<AdminActivityRegistrationResponse> registrations
) {
}
