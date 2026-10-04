package com.scms.core.club.dto;

import java.time.Instant;

public record ClubResponse(
        Long id,
        String name,
        String type,
        String description,
        long memberCount,
        Instant createdAt
) {
}
