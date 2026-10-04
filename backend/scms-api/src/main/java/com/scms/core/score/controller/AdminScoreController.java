package com.scms.core.score.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.score.dto.AdminScoreRecordMutationRequest;
import com.scms.core.score.dto.AdminScoreRecordResponse;
import com.scms.core.score.service.ScoreService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/score-records")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminScoreController {

    private final ScoreService scoreService;
    private final ApiResponseFactory responseFactory;

    public AdminScoreController(ScoreService scoreService, ApiResponseFactory responseFactory) {
        this.scoreService = scoreService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<AdminScoreRecordResponse>> list(
            @RequestParam(name = "clubId", required = false) Long clubId) {
        return responseFactory.success(scoreService.listAdminScores(clubId));
    }

    @PostMapping
    public ApiResponse<AdminScoreRecordResponse> create(
            @Valid @RequestBody AdminScoreRecordMutationRequest request) {
        return responseFactory.success(scoreService.addAdminScore(request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(@PathVariable("recordId") Long recordId) {
        scoreService.deleteAdminScore(recordId);
        return responseFactory.success(null);
    }
}
