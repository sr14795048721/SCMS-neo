package com.scms.core.club.controller;

import com.scms.core.club.dto.ClubCreationRequestResponse;
import com.scms.core.club.service.ClubCreationRequestService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/club-creation-requests")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminClubCreationRequestController {

    private final ClubCreationRequestService clubCreationRequestService;
    private final ApiResponseFactory responseFactory;

    public AdminClubCreationRequestController(ClubCreationRequestService clubCreationRequestService,
                                              ApiResponseFactory responseFactory) {
        this.clubCreationRequestService = clubCreationRequestService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ClubCreationRequestResponse>> list() {
        return responseFactory.success(clubCreationRequestService.listAdminRequests());
    }

    @PostMapping("/{requestId}/approve")
    public ApiResponse<ClubCreationRequestResponse> approve(@PathVariable("requestId") Long requestId) {
        return responseFactory.success(clubCreationRequestService.approveRequest(requestId));
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<ClubCreationRequestResponse> reject(@PathVariable("requestId") Long requestId) {
        return responseFactory.success(clubCreationRequestService.rejectRequest(requestId));
    }
}
