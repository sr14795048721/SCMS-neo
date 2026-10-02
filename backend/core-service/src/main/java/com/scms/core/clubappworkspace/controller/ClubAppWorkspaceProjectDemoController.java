package com.scms.core.clubappworkspace.controller;

import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeResponse;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceProjectDemoService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clubs")
public class ClubAppWorkspaceProjectDemoController {

    private final ClubAppWorkspaceProjectDemoService projectDemoService;
    private final ApiResponseFactory responseFactory;

    public ClubAppWorkspaceProjectDemoController(
            ClubAppWorkspaceProjectDemoService projectDemoService,
            ApiResponseFactory responseFactory
    ) {
        this.projectDemoService = projectDemoService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clubId}/app-workspace/projects/{projectKey}/demo")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<ClubAppWorkspaceProjectDemoResponse> detail(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey
    ) {
        return responseFactory.success(projectDemoService.getVisibleProjectDemo(clubId, projectKey));
    }

    @GetMapping("/{clubId}/app-workspace/projects/{projectKey}/demo/runtime")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<ClubAppWorkspaceProjectDemoRuntimeResponse> runtime(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey
    ) {
        return responseFactory.success(projectDemoService.getVisibleProjectDemoRuntime(clubId, projectKey));
    }

    @PostMapping("/{clubId}/app-workspace/projects/{projectKey}/demo/runtime")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<ClubAppWorkspaceProjectDemoRuntimeResponse> updateRuntime(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey,
            @Valid @RequestBody ClubAppWorkspaceProjectDemoRuntimeRequest request
    ) {
        return responseFactory.success(projectDemoService.saveVisibleProjectDemoRuntime(clubId, projectKey, request));
    }
}
