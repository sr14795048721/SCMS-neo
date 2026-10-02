package com.scms.core.news.dto;

import java.time.Instant;

public record AdminNewsListItemResponse(
        Long id,
        String title,
        String status,
        boolean hasCover,
        String cover,
        String authorName,
        String authorRole,
        long viewCount,
        Instant publishedAt,
        Instant updatedAt
) {
}
