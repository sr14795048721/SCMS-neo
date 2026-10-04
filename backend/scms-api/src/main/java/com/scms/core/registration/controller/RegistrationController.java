package com.scms.core.registration.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.registration.dto.ActivityRegistrationRosterResponse;
import com.scms.core.registration.dto.RegistrationResponse;
import com.scms.core.registration.service.RegistrationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities/{activityId}/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final ApiResponseFactory responseFactory;

    public RegistrationController(RegistrationService registrationService, ApiResponseFactory responseFactory) {
        this.registrationService = registrationService;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ApiResponse<RegistrationResponse> register(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(registrationService.register(activityId));
    }

    @DeleteMapping("/me")
    public ApiResponse<RegistrationResponse> cancel(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(registrationService.cancel(activityId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CLUB_MANAGER','STUDENT')")
    public ApiResponse<List<ActivityRegistrationRosterResponse>> list(@PathVariable("activityId") Long activityId) {
        return responseFactory.success(registrationService.listActive(activityId));
    }
}
