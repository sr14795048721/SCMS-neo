package com.scms.core.competition.dto;

import com.scms.core.competition.domain.CompetitionSourceEntity.SourceType;

import java.time.Instant;

public record CompetitionSourceResponse(
        Long id,
        String name,
        String url,
        SourceType sourceType,
        String wechatName,
        boolean enabled,
        boolean scanEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}
