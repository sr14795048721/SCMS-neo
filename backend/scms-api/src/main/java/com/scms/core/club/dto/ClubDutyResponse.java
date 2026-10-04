package com.scms.core.club.dto;

import java.time.Instant;
import java.util.List;

public record ClubDutyResponse(
        Long id,
        String name,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
}
