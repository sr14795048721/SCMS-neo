package com.scms.core.club.dto;

import java.time.Instant;
import java.util.List;

public record AdminClubDetailResponse(
        Long id,
        String name,
        String type,
        String status,
        String description,
        long memberCount,
        long managerCount,
        Instant createdAt,
        Instant updatedAt,
        List<AdminClubManagerResponse> managers,
        List<AdminClubStudentResponse> students
) {
}
