package com.scms.core.iot.dto;

import java.util.List;

public record IotProjectTelemetryResponse(
        String projectKey,
        String telemetryProjectKey,
        String integrationMode,
        boolean hasTelemetry,
        List<IotProjectTelemetryDeviceResponse> devices,
        List<IotProjectTelemetryEventResponse> recentEvents,
        int eventPage,
        int eventSize,
        long eventTotal,
        boolean hasMoreEvents
) {
}
