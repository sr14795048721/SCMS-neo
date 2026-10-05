package com.scms.core.changelog.controller;

import com.scms.core.changelog.dto.VersionChangelogResponse;
import com.scms.core.changelog.service.VersionChangelogService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/version-changelogs")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CLUB_MANAGER')")
public class VersionChangelogController {

    private final VersionChangelogService versionChangelogService;
    private final ApiResponseFactory responseFactory;

    public VersionChangelogController(VersionChangelogService versionChangelogService,
                                      ApiResponseFactory responseFactory) {
        this.versionChangelogService = versionChangelogService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<VersionChangelogResponse>> list() {
        return responseFactory.success(versionChangelogService.listChangelogs());
    }
}
