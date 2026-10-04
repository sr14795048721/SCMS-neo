package com.scms.core.iot.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.iot.dto.IotProjectTelemetryResponse;
import com.scms.core.iot.service.IotProjectTelemetryQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clubs")
public class IotProjectTelemetryQueryController {

    private final IotProjectTelemetryQueryService queryService;
    private final ApiResponseFactory responseFactory;

    public IotProjectTelemetryQueryController(IotProjectTelemetryQueryService queryService,
                                              ApiResponseFactory responseFactory) {
        this.queryService = queryService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clubId}/app-workspace/projects/{projectKey}/demo/iot-telemetry")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<IotProjectTelemetryResponse> detail(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey,
            @RequestParam(name = "eventPage", defaultValue = "0") Integer eventPage,
            @RequestParam(name = "eventSize", defaultValue = "24") Integer eventSize
    ) {
        return responseFactory.success(queryService.getVisibleProjectTelemetry(clubId, projectKey, eventPage, eventSize));
    }
}
