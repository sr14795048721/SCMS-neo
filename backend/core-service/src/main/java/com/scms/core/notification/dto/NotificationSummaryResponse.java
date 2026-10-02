package com.scms.core.notification.dto;

import java.util.List;

public record NotificationSummaryResponse(
        long unreadCount,
        List<NotificationResponse> latestUnread
) {
}
