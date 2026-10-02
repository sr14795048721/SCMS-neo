package com.scms.core.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String category,
        String title,
        String content,
        String status,
        Instant createdAt,
        String targetPath
) {
}
