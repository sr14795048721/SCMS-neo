package com.scms.core.iot.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Esp32TelemetryResponse(
        boolean ok,
        String requestId,
        Instant serverTime,
        Long telemetryEventId
) {
}
