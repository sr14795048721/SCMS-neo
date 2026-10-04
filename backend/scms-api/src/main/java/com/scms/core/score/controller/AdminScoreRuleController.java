package com.scms.core.score.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.score.dto.AdminScoreRuleMutationRequest;
import com.scms.core.score.dto.ScoreRuleResponse;
import com.scms.core.score.service.ScoreService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/score-rules")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminScoreRuleController {

    private final ScoreService scoreService;
    private final ApiResponseFactory responseFactory;

    public AdminScoreRuleController(ScoreService scoreService, ApiResponseFactory responseFactory) {
        this.scoreService = scoreService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ScoreRuleResponse>> list() {
        return responseFactory.success(scoreService.listAdminRules());
    }

    @PostMapping
    public ApiResponse<ScoreRuleResponse> create(@Valid @RequestBody AdminScoreRuleMutationRequest request) {
        return responseFactory.success(scoreService.createRule(request));
    }

    @PatchMapping("/{ruleId}")
    public ApiResponse<ScoreRuleResponse> update(@PathVariable("ruleId") Long ruleId,
                                                 @Valid @RequestBody AdminScoreRuleMutationRequest request) {
        return responseFactory.success(scoreService.updateRule(ruleId, request));
    }

    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> delete(@PathVariable("ruleId") Long ruleId) {
        scoreService.deleteRule(ruleId);
        return responseFactory.success(null);
    }
}
