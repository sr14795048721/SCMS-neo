package com.scms.core.changelog.controller;

import com.scms.core.changelog.dto.VersionChangelogMutationRequest;
import com.scms.core.changelog.dto.VersionChangelogResponse;
import com.scms.core.changelog.service.VersionChangelogService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/version-changelogs")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminVersionChangelogController {

    private final VersionChangelogService versionChangelogService;
    private final ApiResponseFactory responseFactory;

    public AdminVersionChangelogController(VersionChangelogService versionChangelogService,
                                           ApiResponseFactory responseFactory) {
        this.versionChangelogService = versionChangelogService;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ApiResponse<VersionChangelogResponse> create(@Valid @RequestBody VersionChangelogMutationRequest request) {
        return responseFactory.success(versionChangelogService.createChangelog(request));
    }

    @PatchMapping("/{changelogId}")
    public ApiResponse<VersionChangelogResponse> update(@PathVariable("changelogId") Long changelogId,
                                                        @Valid @RequestBody VersionChangelogMutationRequest request) {
        return responseFactory.success(versionChangelogService.updateChangelog(changelogId, request));
    }

    @DeleteMapping("/{changelogId}")
    public ApiResponse<Void> delete(@PathVariable("changelogId") Long changelogId) {
        versionChangelogService.deleteChangelog(changelogId);
        return responseFactory.success(null);
    }
}
