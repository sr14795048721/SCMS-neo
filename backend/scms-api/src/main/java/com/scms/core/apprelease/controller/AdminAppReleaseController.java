package com.scms.core.apprelease.controller;

import com.scms.core.apprelease.dto.AdminAppReleaseRequest;
import com.scms.core.apprelease.dto.AdminAppReleaseResponse;
import com.scms.core.apprelease.service.AppReleaseService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/app-releases")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminAppReleaseController {

    private final AppReleaseService appReleaseService;
    private final ApiResponseFactory responseFactory;

    public AdminAppReleaseController(AppReleaseService appReleaseService,
                                     ApiResponseFactory responseFactory) {
        this.appReleaseService = appReleaseService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<AdminAppReleaseResponse>> list() {
        return responseFactory.success(appReleaseService.listAdmin());
    }

    @PostMapping
    public ApiResponse<AdminAppReleaseResponse> create(@Valid @RequestBody AdminAppReleaseRequest request) {
        return responseFactory.success(appReleaseService.create(request));
    }

    @PatchMapping("/{releaseId}")
    public ApiResponse<AdminAppReleaseResponse> update(@PathVariable("releaseId") Long releaseId,
                                                       @Valid @RequestBody AdminAppReleaseRequest request) {
        return responseFactory.success(appReleaseService.update(releaseId, request));
    }

    @PostMapping("/{releaseId}/publish")
    public ApiResponse<AdminAppReleaseResponse> publish(@PathVariable("releaseId") Long releaseId) {
        return responseFactory.success(appReleaseService.publish(releaseId));
    }

    @PostMapping("/{releaseId}/retire")
    public ApiResponse<AdminAppReleaseResponse> retire(@PathVariable("releaseId") Long releaseId) {
        return responseFactory.success(appReleaseService.retire(releaseId));
    }
}
