package com.scms.core.apprelease.dto;

import java.time.Instant;

public record AdminAppReleaseResponse(
        Long id,
        String versionName,
        Integer buildNumber,
        String releaseNotes,
        Boolean forceUpdate,
        String androidUrl,
        String harmonyUrl,
        String status,
        Instant publishedAt,
        Long createdBy,
        Long updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
