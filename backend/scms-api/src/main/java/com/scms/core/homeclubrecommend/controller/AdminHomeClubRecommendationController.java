package com.scms.core.homeclubrecommend.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.homeclubrecommend.dto.AdminHomeClubRecommendationResponse;
import com.scms.core.homeclubrecommend.dto.TeacherClubRecommendationGroupResponse;
import com.scms.core.homeclubrecommend.dto.UpdateHomeClubRecommendationsRequest;
import com.scms.core.homeclubrecommend.service.HomeClubRecommendationService;
import com.scms.core.homeclubrecommend.service.TeacherClubRecommendationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/club-recommendations")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminHomeClubRecommendationController {

    private final HomeClubRecommendationService homeClubRecommendationService;
    private final TeacherClubRecommendationService teacherClubRecommendationService;
    private final ApiResponseFactory responseFactory;

    public AdminHomeClubRecommendationController(HomeClubRecommendationService homeClubRecommendationService,
                                                 TeacherClubRecommendationService teacherClubRecommendationService,
                                                 ApiResponseFactory responseFactory) {
        this.homeClubRecommendationService = homeClubRecommendationService;
        this.teacherClubRecommendationService = teacherClubRecommendationService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<AdminHomeClubRecommendationResponse>> list() {
        return responseFactory.success(homeClubRecommendationService.listAdmin());
    }

    @GetMapping("/teacher-submissions")
    public ApiResponse<List<TeacherClubRecommendationGroupResponse>> listTeacherSubmissions() {
        return responseFactory.success(teacherClubRecommendationService.listTeacherSubmissionsForAdmin());
    }

    @PutMapping
    public ApiResponse<List<AdminHomeClubRecommendationResponse>> update(@Valid @RequestBody UpdateHomeClubRecommendationsRequest request) {
        return responseFactory.success(homeClubRecommendationService.updateRecommendations(request.clubIds()));
    }
}
