package com.scms.core.competition.dto;

import com.scms.core.competition.domain.CompetitionLeadEntity.LeadStatus;

import java.time.Instant;

public record CompetitionLeadResponse(
        Long id,
        Long sourceId,
        String sourceName,
        String title,
        String url,
        String snippet,
        Instant detectedAt,
        LeadStatus status,
        Instant createdAt
) {
}
