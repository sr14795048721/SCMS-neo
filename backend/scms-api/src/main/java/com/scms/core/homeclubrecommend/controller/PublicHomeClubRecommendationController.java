package com.scms.core.homeclubrecommend.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.homeclubrecommend.dto.PublicHomeRecommendedClubResponse;
import com.scms.core.homeclubrecommend.service.HomeClubRecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home/recommended-clubs")
public class PublicHomeClubRecommendationController {

    private final HomeClubRecommendationService homeClubRecommendationService;
    private final ApiResponseFactory responseFactory;

    public PublicHomeClubRecommendationController(HomeClubRecommendationService homeClubRecommendationService,
                                                  ApiResponseFactory responseFactory) {
        this.homeClubRecommendationService = homeClubRecommendationService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<PublicHomeRecommendedClubResponse>> list() {
        return responseFactory.success(homeClubRecommendationService.listPublic());
    }
}
