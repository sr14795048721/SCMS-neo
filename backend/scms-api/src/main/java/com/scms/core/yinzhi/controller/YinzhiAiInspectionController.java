package com.scms.core.yinzhi.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionResponse;
import com.scms.core.yinzhi.dto.YinzhiManualConfirmRequest;
import com.scms.core.yinzhi.service.YinzhiAiInspectionQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clubs")
public class YinzhiAiInspectionController {

    private final YinzhiAiInspectionQueryService queryService;
    private final ApiResponseFactory responseFactory;

    public YinzhiAiInspectionController(
            YinzhiAiInspectionQueryService queryService,
            ApiResponseFactory responseFactory
    ) {
        this.queryService = queryService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clubId}/app-workspace/projects/{projectKey}/demo/ai-inspection/latest")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<YinzhiAiInspectionResponse> latest(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey
    ) {
        return responseFactory.success(queryService.getVisibleLatest(clubId, projectKey));
    }

    @PostMapping("/{clubId}/app-workspace/projects/{projectKey}/demo/ai-inspection/commands/{commandId}/manual-confirm")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<YinzhiAiInspectionResponse> manualConfirm(
            @PathVariable("clubId") Long clubId,
            @PathVariable("projectKey") String projectKey,
            @PathVariable("commandId") Long commandId,
            @RequestBody(required = false) YinzhiManualConfirmRequest request
    ) {
        return responseFactory.success(queryService.confirmHighRiskManualHandling(clubId, projectKey, commandId, request));
    }
}
