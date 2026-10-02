package com.scms.core.registration.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.registration.service.RegistrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities/registrations/me")
public class MyRegistrationController {

    private final RegistrationService registrationService;
    private final ApiResponseFactory responseFactory;

    public MyRegistrationController(RegistrationService registrationService, ApiResponseFactory responseFactory) {
        this.registrationService = registrationService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<Long>> listMyRegisteredActivityIds() {
        return responseFactory.success(registrationService.listMyRegisteredActivityIds());
    }
}
