package com.scms.core.club.controller;

import com.scms.core.club.dto.ClubCreationRequestResponse;
import com.scms.core.club.dto.CreateClubCreationRequestRequest;
import com.scms.core.club.service.ClubCreationRequestService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers/me/club-creation-requests")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerClubCreationRequestController {

    private final ClubCreationRequestService clubCreationRequestService;
    private final ApiResponseFactory responseFactory;

    public ManagerClubCreationRequestController(ClubCreationRequestService clubCreationRequestService,
                                                ApiResponseFactory responseFactory) {
        this.clubCreationRequestService = clubCreationRequestService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ClubCreationRequestResponse>> list() {
        return responseFactory.success(clubCreationRequestService.listManagerRequests());
    }

    @PostMapping
    public ApiResponse<ClubCreationRequestResponse> create(@Valid @RequestBody CreateClubCreationRequestRequest request) {
        return responseFactory.success(clubCreationRequestService.createManagerRequest(request));
    }
}
