package com.scms.core.homeclubrecommend.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.homeclubrecommend.dto.TeacherClubRecommendationItemResponse;
import com.scms.core.homeclubrecommend.dto.UpdateTeacherClubRecommendationsRequest;
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
@RequestMapping("/api/v1/managers/me/club-recommendations")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerTeacherClubRecommendationController {

    private final TeacherClubRecommendationService teacherClubRecommendationService;
    private final ApiResponseFactory responseFactory;

    public ManagerTeacherClubRecommendationController(TeacherClubRecommendationService teacherClubRecommendationService,
                                                      ApiResponseFactory responseFactory) {
        this.teacherClubRecommendationService = teacherClubRecommendationService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<TeacherClubRecommendationItemResponse>> list() {
        return responseFactory.success(teacherClubRecommendationService.listManagerRecommendations());
    }

    @PutMapping
    public ApiResponse<List<TeacherClubRecommendationItemResponse>> update(
            @Valid @RequestBody UpdateTeacherClubRecommendationsRequest request
    ) {
        return responseFactory.success(teacherClubRecommendationService.updateManagerRecommendations(request.clubIds()));
    }
}
