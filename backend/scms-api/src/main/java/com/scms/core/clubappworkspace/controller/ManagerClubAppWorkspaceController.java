package com.scms.core.clubappworkspace.controller;

import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceRequest;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceResponse;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceService;
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
public class ManagerClubAppWorkspaceController {

    private final ClubAppWorkspaceService clubAppWorkspaceService;
    private final ApiResponseFactory responseFactory;

    public ManagerClubAppWorkspaceController(ClubAppWorkspaceService clubAppWorkspaceService,
                                             ApiResponseFactory responseFactory) {
        this.clubAppWorkspaceService = clubAppWorkspaceService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clubId}/app-workspace")
    public ApiResponse<ManagerClubAppWorkspaceResponse> detail(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(clubAppWorkspaceService.getManagerWorkspace(clubId));
    }

    @PutMapping("/{clubId}/app-workspace")
    public ApiResponse<ManagerClubAppWorkspaceResponse> update(@PathVariable("clubId") Long clubId,
                                                               @Valid @RequestBody ClubAppWorkspaceRequest request) {
        return responseFactory.success(clubAppWorkspaceService.saveManagerWorkspace(clubId, request));
    }
}
