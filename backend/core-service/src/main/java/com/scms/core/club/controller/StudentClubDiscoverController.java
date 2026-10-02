package com.scms.core.club.controller;

import com.scms.core.club.dto.StudentDiscoverClubsResponse;
import com.scms.core.club.service.StudentClubDiscoverService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me/discover-clubs")
@PreAuthorize("hasRole('STUDENT')")
public class StudentClubDiscoverController {

    private final StudentClubDiscoverService studentClubDiscoverService;
    private final ApiResponseFactory responseFactory;

    public StudentClubDiscoverController(StudentClubDiscoverService studentClubDiscoverService,
                                         ApiResponseFactory responseFactory) {
        this.studentClubDiscoverService = studentClubDiscoverService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<StudentDiscoverClubsResponse> getDiscoverClubs() {
        return responseFactory.success(studentClubDiscoverService.getDiscoverClubs());
    }
}
