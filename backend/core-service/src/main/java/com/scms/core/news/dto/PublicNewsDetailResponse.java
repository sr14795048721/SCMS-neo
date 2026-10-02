package com.scms.core.news.dto;

import java.time.Instant;

public record PublicNewsDetailResponse(
        Long id,
        String title,
        String cover,
        String authorName,
        Instant publishedAt,
        long viewCount,
        String markdownContent
) {
}
