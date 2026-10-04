package com.scms.core.news.dto;

import java.time.Instant;

public record AdminNewsDetailResponse(
        Long id,
        String title,
        String status,
        String markdownContent,
        String cover,
        String authorName,
        String authorRole,
        long viewCount,
        Instant publishedAt,
        Instant coverUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
