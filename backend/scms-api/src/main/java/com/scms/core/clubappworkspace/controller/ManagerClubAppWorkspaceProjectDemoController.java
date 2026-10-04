package com.scms.core.clubappworkspace.controller;

import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoRequest;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoResponse;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceProjectDemoService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/managers/me/clubs")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerClubAppWorkspaceProjectDemoController {

    private final ClubAppWorkspaceProjectDemoService projectDemoService;
    private final ApiResponseFactory responseFactory;

    public ManagerClubAppWorkspaceProjectDemoController(
            ClubAppWorkspaceProjectDemoService projectDemoService,
            ApiResponseFactory responseFactory
    ) {
        this.projectDemoService = projectDemoService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clubId}/app-workspace/projects/{projectKey}/demo")
    public ApiResponse<ManagerClubAppWorkspaceProjectDemoResponse> detail(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey
    ) {
        return responseFactory.success(projectDemoService.getManagerProjectDemo(clubId, projectKey));
    }

    @PutMapping("/{clubId}/app-workspace/projects/{projectKey}/demo")
    public ApiResponse<ManagerClubAppWorkspaceProjectDemoResponse> update(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey,
            @Valid @RequestBody ManagerClubAppWorkspaceProjectDemoRequest request
    ) {
        return responseFactory.success(projectDemoService.saveManagerProjectDemo(clubId, projectKey, request));
    }
}
