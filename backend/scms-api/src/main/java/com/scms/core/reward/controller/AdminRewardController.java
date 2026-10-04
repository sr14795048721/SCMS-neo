package com.scms.core.reward.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.reward.dto.AdminRewardItemResponse;
import com.scms.core.reward.dto.AdminRewardMutationRequest;
import com.scms.core.reward.service.AdminRewardService;
import com.scms.core.reward.service.RewardImageContent;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rewards")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminRewardController {

    private final AdminRewardService adminRewardService;
    private final ApiResponseFactory responseFactory;

    public AdminRewardController(AdminRewardService adminRewardService, ApiResponseFactory responseFactory) {
        this.adminRewardService = adminRewardService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<AdminRewardItemResponse>> list() {
        return responseFactory.success(adminRewardService.listAdminRewards());
    }

    @PostMapping
    public ApiResponse<AdminRewardItemResponse> create(@Valid @RequestBody AdminRewardMutationRequest request) {
        return responseFactory.success(adminRewardService.createReward(request));
    }

    @PatchMapping("/{rewardId}")
    public ApiResponse<AdminRewardItemResponse> update(@PathVariable("rewardId") Long rewardId,
                                                       @Valid @RequestBody AdminRewardMutationRequest request) {
        return responseFactory.success(adminRewardService.updateReward(rewardId, request));
    }

    @PostMapping(path = "/{rewardId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminRewardItemResponse> uploadImage(@PathVariable("rewardId") Long rewardId,
                                                            @RequestPart("file") MultipartFile file) {
        return responseFactory.success(adminRewardService.uploadImage(rewardId, file));
    }

    @GetMapping("/{rewardId}/image/content")
    public ResponseEntity<InputStreamResource> imageContent(@PathVariable("rewardId") Long rewardId) {
        return buildBinaryResponse(adminRewardService.resolveImageContent(rewardId));
    }

    @DeleteMapping("/{rewardId}")
    public ApiResponse<Void> delete(@PathVariable("rewardId") Long rewardId) {
        adminRewardService.deleteReward(rewardId);
        return responseFactory.success(null);
    }

    private ResponseEntity<InputStreamResource> buildBinaryResponse(RewardImageContent mediaContent) {
        try {
            InputStream inputStream = Files.newInputStream(mediaContent.path());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.parseMediaType(mediaContent.contentType()))
                    .contentLength(mediaContent.contentLength())
                    .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
