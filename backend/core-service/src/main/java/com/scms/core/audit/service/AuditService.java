package com.scms.core.audit.service;

import com.scms.core.audit.domain.AuditLogEntity;
import com.scms.core.audit.dto.AuditLogResponse;
import com.scms.core.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void create(String eventType, Long operatorId, String targetType, String targetId, String details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setEventType(eventType);
        entity.setOperatorId(operatorId);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setDetails(details);
        auditLogRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> latest() {
        return auditLogRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getEventType(),
                        log.getOperatorId(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
