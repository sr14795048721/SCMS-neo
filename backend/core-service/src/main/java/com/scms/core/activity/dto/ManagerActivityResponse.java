package com.scms.core.activity.dto;

import java.time.Instant;

public record ManagerActivityResponse(
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
        Instant updatedAt
) {
}
