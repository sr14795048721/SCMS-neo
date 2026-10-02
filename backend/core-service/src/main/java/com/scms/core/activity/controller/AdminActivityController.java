package com.scms.core.activity.controller;

import com.scms.core.activity.dto.AdminActivityDetailResponse;
import com.scms.core.activity.dto.AdminActivityListItemResponse;
import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.service.AdminActivityService;
import com.scms.core.adminuser.dto.AdminUserPageResponse;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/activities")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminActivityController {

    private final AdminActivityService adminActivityService;
    private final ApiResponseFactory responseFactory;

    public AdminActivityController(AdminActivityService adminActivityService, ApiResponseFactory responseFactory) {
        this.adminActivityService = adminActivityService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<AdminUserPageResponse<AdminActivityListItemResponse>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "clubId", required = false) Long clubId,
            @RequestParam(name = "status", defaultValue = "") String status
    ) {
        return responseFactory.success(adminActivityService.list(page, pageSize, keyword, clubId, status));
    }

    @PostMapping
    public ApiResponse<AdminActivityDetailResponse> create(@Valid @RequestBody AdminActivityMutationRequest request) {
        return responseFactory.success(adminActivityService.create(request));
    }

    @GetMapping("/{activityId}")
    public ApiResponse<AdminActivityDetailResponse> detail(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(adminActivityService.getDetail(activityId));
    }

    @PatchMapping("/{activityId}")
    public ApiResponse<AdminActivityDetailResponse> update(@PathVariable("activityId") Long activityId,
                                                           @Valid @RequestBody AdminActivityMutationRequest request) {
        return responseFactory.success(adminActivityService.update(activityId, request));
    }

    @PostMapping("/{activityId}/publish")
    public ApiResponse<AdminActivityDetailResponse> publish(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(adminActivityService.publish(activityId));
    }

    @PostMapping("/{activityId}/close")
    public ApiResponse<AdminActivityDetailResponse> close(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(adminActivityService.close(activityId));
    }

    @DeleteMapping("/{activityId}")
    public ApiResponse<Void> delete(@PathVariable("activityId") Long activityId) {
        adminActivityService.delete(activityId);
        return responseFactory.success(null);
    }
}
