package com.scms.core.clubappworkspace.controller;

import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDetailResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceResponse;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceMaterialContent;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/clubs")
public class ClubAppWorkspaceController {

    private final ClubAppWorkspaceService clubAppWorkspaceService;
    private final ApiResponseFactory responseFactory;

    public ClubAppWorkspaceController(ClubAppWorkspaceService clubAppWorkspaceService,
                                      ApiResponseFactory responseFactory) {
        this.clubAppWorkspaceService = clubAppWorkspaceService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clubId}/app-workspace")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<ClubAppWorkspaceResponse> detail(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(clubAppWorkspaceService.getVisibleWorkspace(clubId));
    }

    @GetMapping("/{clubId}/app-workspace/projects/{projectKey}")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ApiResponse<ClubAppWorkspaceProjectDetailResponse> projectDetail(@PathVariable("clubId") Long clubId,
                                                                            @PathVariable("projectKey") String projectKey) {
        return responseFactory.success(clubAppWorkspaceService.getVisibleProjectDetail(clubId, projectKey));
    }

    @GetMapping("/{clubId}/app-workspace/projects/{projectKey}/materials/{materialId}/download")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_MANAGER')")
    public ResponseEntity<InputStreamResource> downloadMaterial(@PathVariable("clubId") Long clubId,
                                                                @PathVariable("projectKey") String projectKey,
                                                                @PathVariable("materialId") Long materialId) {
        return buildBinaryResponse(clubAppWorkspaceService.downloadVisibleMaterial(clubId, projectKey, materialId));
    }

    private ResponseEntity<InputStreamResource> buildBinaryResponse(ClubAppWorkspaceMaterialContent mediaContent) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(mediaContent.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType(mediaContent.contentType()));
        if (mediaContent.contentLength() >= 0) {
            builder.contentLength(mediaContent.contentLength());
        }
        return builder.body(new InputStreamResource(mediaContent.inputStream()));
    }
}
