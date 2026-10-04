package com.scms.core.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.audit.service.AuditService;
import com.scms.core.notification.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DomainEventDispatcher {

    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public DomainEventDispatcher(NotificationService notificationService,
                                 AuditService auditService,
                                 ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public void dispatch(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        Long userId = longValue(payload.get("userId"));
        Long operatorId = longValue(payload.get("operatorId"));
        String details = toJson(payload);

        switch (event.topic()) {
            case DomainEventTopics.REGISTRATION_CREATED -> {
                if (userId != null) {
                    notificationService.create(userId, "REGISTRATION", "报名成功", "你已成功报名活动。");
                }
                auditService.create(event.topic(), operatorId, "activity", stringValue(payload.get("activityId")), details);
            }
            case DomainEventTopics.REGISTRATION_CANCELED -> {
                if (userId != null) {
                    notificationService.create(userId, "REGISTRATION", "报名取消", "你已取消报名活动。");
                }
                auditService.create(event.topic(), operatorId, "activity", stringValue(payload.get("activityId")), details);
            }
            case DomainEventTopics.ACTIVITY_PUBLISHED ->
                    auditService.create(event.topic(), operatorId, "activity", stringValue(payload.get("activityId")), details);
            default ->
                    auditService.create(event.topic(), operatorId, "unknown", null, details);
        }
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }
}
