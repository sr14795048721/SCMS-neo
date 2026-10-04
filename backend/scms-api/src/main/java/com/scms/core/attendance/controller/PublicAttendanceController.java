package com.scms.core.attendance.controller;

import com.scms.core.attendance.dto.PublicAttendanceSessionResponse;
import com.scms.core.attendance.dto.PublicAttendanceSignRequest;
import com.scms.core.attendance.dto.PublicAttendanceSignResponse;
import com.scms.core.attendance.service.PublicAttendanceService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/attendance/sessions")
public class PublicAttendanceController {

    private final PublicAttendanceService publicAttendanceService;
    private final ApiResponseFactory responseFactory;

    public PublicAttendanceController(PublicAttendanceService publicAttendanceService,
                                      ApiResponseFactory responseFactory) {
        this.publicAttendanceService = publicAttendanceService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{shareToken}")
    public ApiResponse<PublicAttendanceSessionResponse> getSession(@PathVariable("shareToken") String shareToken) {
        return responseFactory.success(publicAttendanceService.getSession(shareToken));
    }

    @PostMapping("/{shareToken}/sign")
    public ApiResponse<PublicAttendanceSignResponse> sign(@PathVariable("shareToken") String shareToken,
                                                          @Valid @RequestBody PublicAttendanceSignRequest request) {
        return responseFactory.success(publicAttendanceService.sign(shareToken, request));
    }
}
