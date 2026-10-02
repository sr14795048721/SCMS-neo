package com.scms.core.club.controller;

import com.scms.core.club.dto.ClubResponse;
import com.scms.core.club.service.ClubService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ApiResponseFactory responseFactory;

    public ClubController(ClubService clubService, ApiResponseFactory responseFactory) {
        this.clubService = clubService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ClubResponse>> list() {
        return responseFactory.success(clubService.list());
    }
}
