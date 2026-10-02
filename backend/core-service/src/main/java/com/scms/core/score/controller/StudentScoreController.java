package com.scms.core.score.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.score.dto.ClubScoreRankingResponse;
import com.scms.core.score.dto.ScoreSummaryResponse;
import com.scms.core.score.dto.SchoolScoreRankingResponse;
import com.scms.core.score.service.ScoreService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentScoreController {

    private final ScoreService scoreService;
    private final ApiResponseFactory responseFactory;

    public StudentScoreController(ScoreService scoreService, ApiResponseFactory responseFactory) {
        this.scoreService = scoreService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/score-summary")
    public ApiResponse<ScoreSummaryResponse> getSummary() {
        return responseFactory.success(scoreService.getMySummary());
    }

    @GetMapping("/club-rankings")
    public ApiResponse<List<ClubScoreRankingResponse>> getClubRankings(@RequestParam("clubId") Long clubId,
                                                                       @RequestParam(name = "range", required = false) String range) {
        return responseFactory.success(scoreService.getClubRankings(clubId, range));
    }

    @GetMapping("/school-rankings")
    public ApiResponse<List<SchoolScoreRankingResponse>> getSchoolRankings() {
        return responseFactory.success(scoreService.getSchoolRankings());
    }
}
