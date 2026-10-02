package com.scms.core.iot.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CyclingGuardianTelemetryRequest(
        @NotBlank @Size(max = 128) String gatewayId,
        @NotBlank @Size(max = 128) String sourceDeviceId,
        @NotBlank @Size(max = 64) String messageType,
        @NotBlank @Size(max = 64) String transport,
        @NotNull Instant reportedAt,
        @NotNull @PositiveOrZero Long jetsonUptimeMs,
        @NotNull @PositiveOrZero Long uploadSequence,
        @NotNull JsonNode payload
) {

    @AssertTrue(message = "message_type must be telemetry")
    public boolean isMessageTypeValid() {
        return "telemetry".equals(messageType);
    }

    @AssertTrue(message = "transport must be jetson_http")
    public boolean isTransportValid() {
        return "jetson_http".equals(transport);
    }

    @AssertTrue(message = "payload must be a JSON object")
    public boolean isPayloadObject() {
        return payload != null && payload.isObject() && !payload.isEmpty();
    }
}
