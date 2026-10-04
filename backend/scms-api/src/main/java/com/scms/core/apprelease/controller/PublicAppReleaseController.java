package com.scms.core.apprelease.controller;

import com.scms.core.apprelease.dto.AppReleaseCheckResponse;
import com.scms.core.apprelease.service.AppReleaseService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/app-releases")
public class PublicAppReleaseController {

    private final AppReleaseService appReleaseService;
    private final ApiResponseFactory responseFactory;

    public PublicAppReleaseController(AppReleaseService appReleaseService,
                                      ApiResponseFactory responseFactory) {
        this.appReleaseService = appReleaseService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/check")
    public ApiResponse<AppReleaseCheckResponse> check(@RequestParam("platform") String platform,
                                                      @RequestParam("versionName") String versionName,
                                                      @RequestParam("buildNumber") Integer buildNumber) {
        return responseFactory.success(appReleaseService.checkLatest(platform, versionName, buildNumber));
    }
}
