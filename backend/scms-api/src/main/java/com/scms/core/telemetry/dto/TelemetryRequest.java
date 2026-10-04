package com.scms.core.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TelemetryRequest(
        @NotBlank @Size(max = 64) String visitorId,
        @NotBlank @Size(max = 64) String sessionId,
        @NotBlank @Size(max = 255) String path
) {
}
