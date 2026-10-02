package com.scms.core.clubappworkspace.controller;

import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRiskSyncRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeResponse;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceProjectDemoService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integration/project-demo")
public class ClubAppWorkspaceProjectDemoBridgeController {

    private static final String SECRET_HEADER = "X-MHV-Bridge-Secret";

    private final ClubAppWorkspaceProjectDemoService projectDemoService;
    private final ApiResponseFactory responseFactory;
    private final String bridgeSecret;

    public ClubAppWorkspaceProjectDemoBridgeController(
            ClubAppWorkspaceProjectDemoService projectDemoService,
            ApiResponseFactory responseFactory,
            @Value("${project-demo.bridge-secret:}") String bridgeSecret
    ) {
        this.projectDemoService = projectDemoService;
        this.responseFactory = responseFactory;
        this.bridgeSecret = bridgeSecret == null ? "" : bridgeSecret.trim();
    }

    @PostMapping("/risk-sync")
    public ApiResponse<ClubAppWorkspaceProjectDemoRuntimeResponse> syncRiskRuntime(
            @RequestHeader(value = SECRET_HEADER, required = false) String providedSecret,
            @Valid @RequestBody ClubAppWorkspaceProjectDemoRiskSyncRequest request
    ) {
        if (bridgeSecret.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "project demo bridge secret not configured");
        }
        String normalizedProvidedSecret = providedSecret == null ? "" : providedSecret.trim();
        if (!bridgeSecret.equals(normalizedProvidedSecret)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid project demo bridge secret");
        }
        return responseFactory.success(projectDemoService.saveBridgeProjectDemoRiskRuntime(request));
    }
}
