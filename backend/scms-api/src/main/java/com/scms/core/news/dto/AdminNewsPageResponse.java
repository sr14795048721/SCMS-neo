package com.scms.core.news.dto;

import java.util.List;

public record AdminNewsPageResponse(
        List<AdminNewsListItemResponse> items,
        int page,
        int pageSize,
        long total,
        int totalPages
) {
}
