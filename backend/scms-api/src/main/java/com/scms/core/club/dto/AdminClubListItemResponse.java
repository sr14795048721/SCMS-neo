package com.scms.core.club.dto;

import java.time.Instant;

public record AdminClubListItemResponse(
        Long id,
        String name,
        String type,
        String status,
        String description,
        long memberCount,
        long managerCount,
        Instant createdAt,
        Instant updatedAt
) {
}
