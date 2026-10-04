package com.scms.core.iot.controller;

import com.scms.core.common.web.RequestIdUtil;
import com.scms.core.iot.dto.CyclingGuardianTelemetryRequest;
import com.scms.core.iot.dto.Esp32TelemetryResponse;
import com.scms.core.iot.service.IotDeviceTelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/iot/cycling-guardian")
public class CyclingGuardianTelemetryController {

    private final IotDeviceTelemetryService iotDeviceTelemetryService;

    public CyclingGuardianTelemetryController(IotDeviceTelemetryService iotDeviceTelemetryService) {
        this.iotDeviceTelemetryService = iotDeviceTelemetryService;
    }

    @PostMapping("/telemetry")
    public Esp32TelemetryResponse ingest(
            @Valid @RequestBody CyclingGuardianTelemetryRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        iotDeviceTelemetryService.ingest(request, userAgent);
        return new Esp32TelemetryResponse(true, RequestIdUtil.resolveFromMdcOrGenerate(), Instant.now(), null);
    }
}
