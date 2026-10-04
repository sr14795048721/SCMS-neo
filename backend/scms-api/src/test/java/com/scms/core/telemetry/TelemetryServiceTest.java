package com.scms.core.telemetry;

import com.scms.core.telemetry.dto.TelemetryRequest;
import com.scms.core.telemetry.repository.VisitEventRepository;
import com.scms.core.telemetry.repository.VisitorSessionRepository;
import com.scms.core.telemetry.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TelemetryServiceTest {

    private VisitEventRepository visitEventRepository;
    private VisitorSessionRepository visitorSessionRepository;
    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        visitEventRepository = mock(VisitEventRepository.class);
        visitorSessionRepository = mock(VisitorSessionRepository.class);
        telemetryService = new TelemetryService(visitEventRepository, visitorSessionRepository);
    }

    @Test
    void recordPageViewShouldPersistEventAndCreateSession() {
        TelemetryRequest request = new TelemetryRequest("visitor-1", "session-1", "/sys-admin");

        telemetryService.recordPageView(request, "Mozilla/5.0", 9L);

        verify(visitEventRepository).save(any());
        verify(visitorSessionRepository).upsertSession(
                eq("session-1"),
                eq("visitor-1"),
                eq(9L),
                eq("/sys-admin"),
                any(),
                eq("Mozilla/5.0")
        );
    }

    @Test
    void recordHeartbeatShouldOnlyUpdateSession() {
        TelemetryRequest request = new TelemetryRequest("visitor-2", "session-2", "/student");

        telemetryService.recordHeartbeat(request, "TestAgent", null);

        verify(visitEventRepository, never()).save(any());
        verify(visitorSessionRepository).upsertSession(
                eq("session-2"),
                eq("visitor-2"),
                eq(null),
                eq("/student"),
                any(),
                eq("TestAgent")
        );
    }

    @Test
    void recordHeartbeatShouldNormalizePathBeforeUpsert() {
        TelemetryRequest request = new TelemetryRequest("visitor-3", "session-3", "sys-admin");

        telemetryService.recordHeartbeat(request, null, null);

        verify(visitorSessionRepository).upsertSession(
                eq("session-3"),
                eq("visitor-3"),
                eq(null),
                eq("/sys-admin"),
                any(),
                eq(null)
        );
    }
}
