package com.scms.core.iot.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record IotProjectTelemetryDeviceResponse(
        String deviceId,
        String gatewayType,
        String firmware,
        String mac,
        String latestEvent,
        Long uploadSequence,
        Long sentAtMs,
        Long gatewayUptimeMs,
        String pendingReason,
        boolean wifiConnected,
        String ip,
        Integer rssi,
        Instant lastReceivedAt,
        JsonNode edgeInference,
        JsonNode payload
) {
}
