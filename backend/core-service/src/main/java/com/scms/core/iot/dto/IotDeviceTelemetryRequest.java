package com.scms.core.iot.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record IotDeviceTelemetryRequest(
        @NotBlank @Size(max = 64) String schema,
        @NotBlank @Size(max = 64) String project,
        @NotBlank @Size(max = 64) String event,
        @NotNull @PositiveOrZero Long sentAtMs,
        @Valid @NotNull Device device,
        @Valid @NotNull Network network,
        @Valid @NotNull Gateway gateway,
        @Valid @NotNull Atmega atmega,
        @Valid @NotNull Vision vision,
        @Valid @NotNull Diagnostics diagnostics
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Device(
            @NotBlank @Size(max = 128) String deviceId,
            @NotBlank @Size(max = 64) String gatewayType,
            @Size(max = 128) String firmware,
            @Size(max = 32) String mac
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Network(
            @Size(max = 64) String ssid,
            @NotNull Boolean wifiConnected,
            @Size(max = 64) String ip,
            Integer rssi
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Gateway(
            @NotNull @PositiveOrZero Long uptimeMs,
            @NotNull @PositiveOrZero Long uploadSequence,
            @Size(max = 64) String pendingReason
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Atmega(
            @NotNull Boolean online,
            @NotNull Boolean valid,
            @NotNull @PositiveOrZero Long receivedAtMs,
            @NotNull @PositiveOrZero Long seq,
            @NotNull @PositiveOrZero Long uptimeMs,
            @NotNull Boolean doorOpen,
            @NotNull Boolean garageOpen,
            @NotNull Boolean fanOn,
            @NotNull Boolean auxLedOn,
            @NotNull Boolean oledReady,
            @Size(max = 64) String rgbMode,
            @Size(max = 64) String lastCommand
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Vision(
            @NotNull Boolean online,
            @NotNull Boolean connected,
            @NotNull Boolean learned,
            @NotNull Boolean hasTarget,
            @NotNull @PositiveOrZero Long receivedAtMs,
            @NotNull @PositiveOrZero Long resultSequence,
            @Size(max = 64) String resultType,
            @NotNull Integer id,
            @NotNull Integer xCenter,
            @NotNull Integer yCenter,
            @NotNull Integer width,
            @NotNull Integer height,
            @NotNull Integer xOrigin,
            @NotNull Integer yOrigin,
            @NotNull Integer xTarget,
            @NotNull Integer yTarget
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Diagnostics(
            @Size(max = 512) String lastParseError,
            Integer lastHttpCode,
            @Size(max = 64) String lastUploadReason
    ) {
    }
}
