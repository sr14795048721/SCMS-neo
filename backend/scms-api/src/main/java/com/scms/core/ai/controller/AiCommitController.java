package com.scms.core.ai.controller;

import com.scms.core.ai.dto.AiCommitRequest;
import com.scms.core.ai.dto.AiCommitResponse;
import com.scms.core.ai.service.AiCommitService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiCommitController {

    private final AiCommitService aiCommitService;
    private final ApiResponseFactory responseFactory;

    public AiCommitController(AiCommitService aiCommitService,
                              ApiResponseFactory responseFactory) {
        this.aiCommitService = aiCommitService;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/commit")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CLUB_MANAGER','STUDENT')")
    public ApiResponse<AiCommitResponse> commit(@Valid @RequestBody AiCommitRequest request) {
        return responseFactory.success(aiCommitService.completeCommit(request.commit()));
    }
}
