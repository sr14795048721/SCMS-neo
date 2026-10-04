package com.scms.core.notification.dto;

import java.util.List;

public record NotificationFeedResponse(
        List<NotificationResponse> items,
        int page,
        int pageSize,
        long total,
        int totalPages
) {
}
