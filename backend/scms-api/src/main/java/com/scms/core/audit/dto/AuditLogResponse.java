package com.scms.core.audit.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String eventType,
        Long operatorId,
        String targetType,
        String targetId,
        String details,
        Instant createdAt
) {
}
