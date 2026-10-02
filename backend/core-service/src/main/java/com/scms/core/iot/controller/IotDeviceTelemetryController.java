package com.scms.core.iot.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.iot.dto.IotDeviceTelemetryIngestResponse;
import com.scms.core.iot.dto.IotDeviceTelemetryRequest;
import com.scms.core.iot.service.IotDeviceTelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iot/v1")
public class IotDeviceTelemetryController {

    private final IotDeviceTelemetryService iotDeviceTelemetryService;
    private final ApiResponseFactory responseFactory;

    public IotDeviceTelemetryController(IotDeviceTelemetryService iotDeviceTelemetryService,
                                        ApiResponseFactory responseFactory) {
        this.iotDeviceTelemetryService = iotDeviceTelemetryService;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/device-telemetry")
    public ApiResponse<IotDeviceTelemetryIngestResponse> ingest(
            @Valid @RequestBody IotDeviceTelemetryRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        return responseFactory.success(iotDeviceTelemetryService.ingest(request, userAgent));
    }
}
