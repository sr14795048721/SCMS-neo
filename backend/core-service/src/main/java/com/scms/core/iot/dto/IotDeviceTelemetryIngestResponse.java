package com.scms.core.iot.dto;

public record IotDeviceTelemetryIngestResponse(
        boolean ok,
        String message,
        Long telemetryEventId,
        Long inspectionRunId
) {
    public IotDeviceTelemetryIngestResponse(boolean ok, String message) {
        this(ok, message, null, null);
    }
}
