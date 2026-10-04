package com.scms.core.activity.controller;

import com.scms.core.activity.dto.ActivityResponse;
import com.scms.core.activity.service.ActivityService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final ApiResponseFactory responseFactory;

    public ActivityController(ActivityService activityService, ApiResponseFactory responseFactory) {
        this.activityService = activityService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ActivityResponse>> list() {
        return responseFactory.success(activityService.list());
    }
}
