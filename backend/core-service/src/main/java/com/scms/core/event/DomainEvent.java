package com.scms.core.event;

import java.time.Instant;
import java.util.Map;

public record DomainEvent(
        String topic,
        String key,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
