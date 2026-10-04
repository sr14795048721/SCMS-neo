package com.scms.core.activity.dto;

import java.time.Instant;

public record ActivityResponse(
        Long id,
        Long clubId,
        String title,
        String description,
        String location,
        Instant startTime,
        Instant endTime,
        int capacity,
        String status,
        Long createdBy,
        Instant createdAt
) {
}
