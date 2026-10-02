package com.scms.core.telemetry.service;

import com.scms.core.telemetry.domain.VisitEventEntity;
import com.scms.core.telemetry.dto.TelemetryRequest;
import com.scms.core.telemetry.repository.VisitEventRepository;
import com.scms.core.telemetry.repository.VisitorSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TelemetryService {

    private final VisitEventRepository visitEventRepository;
    private final VisitorSessionRepository visitorSessionRepository;

    public TelemetryService(VisitEventRepository visitEventRepository,
                            VisitorSessionRepository visitorSessionRepository) {
        this.visitEventRepository = visitEventRepository;
        this.visitorSessionRepository = visitorSessionRepository;
    }

    @Transactional
    public void recordPageView(TelemetryRequest request, String userAgent, Long userId) {
        VisitEventEntity event = new VisitEventEntity();
        event.setVisitorId(normalizeId(request.visitorId()));
        event.setSessionId(normalizeId(request.sessionId()));
        event.setUserId(userId);
        event.setPath(normalizePath(request.path()));
        visitEventRepository.save(event);

        upsertSession(request, userAgent, userId);
    }

    @Transactional
    public void recordHeartbeat(TelemetryRequest request, String userAgent, Long userId) {
        upsertSession(request, userAgent, userId);
    }

    private void upsertSession(TelemetryRequest request, String userAgent, Long userId) {
        Instant now = Instant.now();
        String normalizedSessionId = normalizeId(request.sessionId());
        String normalizedVisitorId = normalizeId(request.visitorId());
        String normalizedPath = normalizePath(request.path());
        String normalizedUserAgent = normalizeUserAgent(userAgent);
        visitorSessionRepository.upsertSession(
                normalizedSessionId,
                normalizedVisitorId,
                userId,
                normalizedPath,
                now,
                normalizedUserAgent
        );
    }

    private String normalizeId(String value) {
        return String.valueOf(value).trim();
    }

    private String normalizePath(String value) {
        String path = String.valueOf(value).trim();
        if (path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String normalizeUserAgent(String userAgent) {
        String value = String.valueOf(userAgent == null ? "" : userAgent).trim();
        if (value.length() <= 512) {
            return value.isEmpty() ? null : value;
        }
        return value.substring(0, 512);
    }
}
