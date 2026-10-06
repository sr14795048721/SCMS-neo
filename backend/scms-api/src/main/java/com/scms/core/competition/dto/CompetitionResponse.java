package com.scms.core.competition.dto;

import com.scms.core.competition.domain.CompetitionEntity.CompetitionStatus;

import java.time.Instant;

public record CompetitionResponse(
        Long id,
        String name,
        String category,
        String participantScope,
        Instant applyDeadline,
        Instant startDate,
        Instant endDate,
        String url,
        Long sourceId,
        String sourceName,
        CompetitionStatus status,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
