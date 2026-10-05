package com.scms.core.changelog.dto;

import java.time.Instant;

public record VersionChangelogResponse(
        Long id,
        String version,
        String title,
        String content,
        Instant releasedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
