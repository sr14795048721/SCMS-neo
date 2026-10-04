package com.scms.core.score.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.score.dto.ClubScoreRankingResponse;
import com.scms.core.score.dto.ClubScoreRecordMutationRequest;
import com.scms.core.score.dto.ClubScoreRecordResponse;
import com.scms.core.score.dto.ScoreRuleResponse;
import com.scms.core.score.service.ScoreService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers/me/clubs/{clubId}")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerScoreController {

    private final ScoreService scoreService;
    private final ApiResponseFactory responseFactory;

    public ManagerScoreController(ScoreService scoreService, ApiResponseFactory responseFactory) {
        this.scoreService = scoreService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/score-rules")
    public ApiResponse<List<ScoreRuleResponse>> listRules(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(scoreService.listClubRules(clubId));
    }

    @GetMapping("/score-records")
    public ApiResponse<List<ClubScoreRecordResponse>> listRecords(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(scoreService.listClubScores(clubId));
    }

    @PostMapping("/score-records")
    public ApiResponse<ClubScoreRecordResponse> createRecord(@PathVariable("clubId") Long clubId,
                                                             @Valid @RequestBody ClubScoreRecordMutationRequest request) {
        return responseFactory.success(scoreService.addClubScore(clubId, request));
    }

    @GetMapping("/score-rankings")
    public ApiResponse<List<ClubScoreRankingResponse>> listRankings(@PathVariable("clubId") Long clubId,
                                                                    @RequestParam(name = "range", defaultValue = "all") String range) {
        return responseFactory.success(scoreService.getClubRankingsForManager(clubId, range));
    }
}
