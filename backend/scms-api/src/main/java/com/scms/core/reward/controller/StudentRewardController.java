package com.scms.core.reward.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.reward.dto.StudentRewardItemResponse;
import com.scms.core.reward.dto.StudentRewardOrderCreateRequest;
import com.scms.core.reward.dto.StudentRewardOrderResponse;
import com.scms.core.reward.service.RewardImageContent;
import com.scms.core.reward.service.StudentRewardService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentRewardController {

    private final StudentRewardService studentRewardService;
    private final ApiResponseFactory responseFactory;

    public StudentRewardController(StudentRewardService studentRewardService, ApiResponseFactory responseFactory) {
        this.studentRewardService = studentRewardService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/rewards")
    public ApiResponse<List<StudentRewardItemResponse>> listRewards() {
        return responseFactory.success(studentRewardService.listRewards());
    }

    @GetMapping("/reward-orders")
    public ApiResponse<List<StudentRewardOrderResponse>> listOrders() {
        return responseFactory.success(studentRewardService.listMyOrders());
    }

    @PostMapping("/reward-orders")
    public ApiResponse<StudentRewardOrderResponse> createOrder(@Valid @RequestBody StudentRewardOrderCreateRequest request) {
        return responseFactory.success(studentRewardService.createOrder(request));
    }

    @GetMapping("/rewards/{rewardId}/image/content")
    public ResponseEntity<InputStreamResource> imageContent(@PathVariable("rewardId") Long rewardId) {
        return buildBinaryResponse(studentRewardService.resolveImageContent(rewardId));
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
