package com.scms.core.iot.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record IotProjectTelemetryEventResponse(
        Long id,
        String deviceId,
        String event,
        Long uploadSequence,
        Long sentAtMs,
        Instant receivedAt,
        String pendingReason,
        boolean wifiConnected,
        String ip,
        Integer rssi,
        JsonNode edgeInference,
        JsonNode payload
) {
}
