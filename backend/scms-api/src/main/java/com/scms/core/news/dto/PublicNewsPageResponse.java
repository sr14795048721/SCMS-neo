package com.scms.core.news.dto;

import java.util.List;

public record PublicNewsPageResponse(
        List<PublicNewsListItemResponse> items,
        int page,
        int pageSize,
        long total,
        int totalPages
) {
}
