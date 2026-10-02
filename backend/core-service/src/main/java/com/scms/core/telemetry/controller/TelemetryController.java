package com.scms.core.telemetry.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.security.JwtService;
import com.scms.core.telemetry.dto.TelemetryRequest;
import com.scms.core.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final ApiResponseFactory responseFactory;
    private final JwtService jwtService;

    public TelemetryController(TelemetryService telemetryService,
                               ApiResponseFactory responseFactory,
                               JwtService jwtService) {
        this.telemetryService = telemetryService;
        this.responseFactory = responseFactory;
        this.jwtService = jwtService;
    }

    @PostMapping("/page-view")
    public ApiResponse<Void> pageView(@Valid @RequestBody TelemetryRequest request,
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                      String authorization,
                                      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false)
                                      String userAgent) {
        telemetryService.recordPageView(request, userAgent, resolveUserId(authorization));
        return responseFactory.success(null);
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@Valid @RequestBody TelemetryRequest request,
                                       @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                       String authorization,
                                       @RequestHeader(value = HttpHeaders.USER_AGENT, required = false)
                                       String userAgent) {
        telemetryService.recordHeartbeat(request, userAgent, resolveUserId(authorization));
        return responseFactory.success(null);
    }

    private Long resolveUserId(String authorization) {
        String token = JwtService.extractBearerToken(authorization);
        if (token == null) {
            return null;
        }
        try {
            return jwtService.parseAccessToken(token).userId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
