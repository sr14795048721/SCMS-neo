package com.scms.core.club.controller;

import com.scms.core.club.dto.ManagerClubJoinRequestResponse;
import com.scms.core.club.dto.StudentClubJoinRequestCreateRequest;
import com.scms.core.club.service.ManagerClubService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me/club-join-requests")
@PreAuthorize("hasRole('STUDENT')")
public class StudentClubJoinRequestController {

    private final ManagerClubService managerClubService;
    private final ApiResponseFactory responseFactory;

    public StudentClubJoinRequestController(ManagerClubService managerClubService, ApiResponseFactory responseFactory) {
        this.managerClubService = managerClubService;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ApiResponse<ManagerClubJoinRequestResponse> create(@Valid @RequestBody StudentClubJoinRequestCreateRequest request) {
        return responseFactory.success(managerClubService.createStudentJoinRequest(request));
    }
}
