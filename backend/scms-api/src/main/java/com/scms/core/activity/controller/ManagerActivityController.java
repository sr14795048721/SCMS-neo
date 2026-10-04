package com.scms.core.activity.controller;

import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.dto.ManagerActivityResponse;
import com.scms.core.activity.service.ManagerActivityService;
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
@RequestMapping("/api/v1/managers/me/activities")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerActivityController {

    private final ManagerActivityService managerActivityService;
    private final ApiResponseFactory responseFactory;

    public ManagerActivityController(ManagerActivityService managerActivityService, ApiResponseFactory responseFactory) {
        this.managerActivityService = managerActivityService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ManagerActivityResponse>> list() {
        return responseFactory.success(managerActivityService.listManagedActivities());
    }

    @PostMapping
    public ApiResponse<ManagerActivityResponse> create(@Valid @RequestBody AdminActivityMutationRequest request) {
        return responseFactory.success(managerActivityService.create(request));
    }

    @PatchMapping("/{activityId}")
    public ApiResponse<ManagerActivityResponse> update(@PathVariable("activityId") Long activityId,
                                                       @Valid @RequestBody AdminActivityMutationRequest request) {
        return responseFactory.success(managerActivityService.update(activityId, request));
    }

    @PostMapping("/{activityId}/publish")
    public ApiResponse<ManagerActivityResponse> publish(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(managerActivityService.publish(activityId));
    }

    @PostMapping("/{activityId}/close")
    public ApiResponse<ManagerActivityResponse> close(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(managerActivityService.close(activityId));
    }
}
