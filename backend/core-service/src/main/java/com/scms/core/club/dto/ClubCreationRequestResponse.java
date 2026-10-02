package com.scms.core.club.dto;

import java.time.Instant;

public record ClubCreationRequestResponse(
        Long id,
        Long applicantManagerUserId,
        String applicantName,
        String applicantManagerNo,
        String name,
        String type,
        String description,
        String applyReason,
        String status,
        Long reviewedBy,
        String reviewedByName,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
