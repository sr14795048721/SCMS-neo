package com.scms.core.activity.controller;

import com.scms.core.activity.dto.ActivityResponse;
import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.service.StudentActivityService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me/activities")
@PreAuthorize("hasRole('STUDENT')")
public class StudentActivityController {

    private final StudentActivityService studentActivityService;
    private final ApiResponseFactory responseFactory;

    public StudentActivityController(StudentActivityService studentActivityService,
                                     ApiResponseFactory responseFactory) {
        this.studentActivityService = studentActivityService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ActivityResponse>> list() {
        return responseFactory.success(studentActivityService.listMyClubActivities());
    }

    @PatchMapping("/{activityId}")
    public ApiResponse<ActivityResponse> update(@PathVariable("activityId") Long activityId,
                                                @Valid @RequestBody AdminActivityMutationRequest request) {
        return responseFactory.success(studentActivityService.update(activityId, request));
    }
}
