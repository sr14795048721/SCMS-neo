package com.scms.core.attendance.controller;

import com.scms.core.attendance.dto.ManagerAttendanceMarkRequest;
import com.scms.core.attendance.dto.ManagerAttendanceBulkMarkRequest;
import com.scms.core.attendance.dto.ManagerAttendanceSessionCreateRequest;
import com.scms.core.attendance.dto.ManagerAttendanceSessionDetailResponse;
import com.scms.core.attendance.dto.ManagerAttendanceSessionSummaryResponse;
import com.scms.core.attendance.service.ManagerAttendanceSessionService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers/me/clubs/{clubId}/attendance-sessions")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerAttendanceSessionController {

    private final ManagerAttendanceSessionService managerAttendanceSessionService;
    private final ApiResponseFactory responseFactory;

    public ManagerAttendanceSessionController(ManagerAttendanceSessionService managerAttendanceSessionService,
                                              ApiResponseFactory responseFactory) {
        this.managerAttendanceSessionService = managerAttendanceSessionService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ManagerAttendanceSessionSummaryResponse>> listSessions(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(managerAttendanceSessionService.listSessions(clubId));
    }

    @PostMapping
    public ApiResponse<ManagerAttendanceSessionDetailResponse> createSession(@PathVariable("clubId") Long clubId,
                                                                             @Valid @RequestBody ManagerAttendanceSessionCreateRequest request) {
        return responseFactory.success(managerAttendanceSessionService.createSession(clubId, request));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<ManagerAttendanceSessionDetailResponse> getSession(@PathVariable("clubId") Long clubId,
                                                                          @PathVariable("sessionId") Long sessionId) {
        return responseFactory.success(managerAttendanceSessionService.getSession(clubId, sessionId));
    }

    @PostMapping("/{sessionId}/check-in")
    public ApiResponse<ManagerAttendanceSessionDetailResponse> checkIn(@PathVariable("clubId") Long clubId,
                                                                       @PathVariable("sessionId") Long sessionId,
                                                                       @Valid @RequestBody ManagerAttendanceMarkRequest request) {
        return responseFactory.success(managerAttendanceSessionService.checkIn(clubId, sessionId, request));
    }

    @PostMapping("/{sessionId}/check-out")
    public ApiResponse<ManagerAttendanceSessionDetailResponse> checkOut(@PathVariable("clubId") Long clubId,
                                                                        @PathVariable("sessionId") Long sessionId,
                                                                        @Valid @RequestBody ManagerAttendanceMarkRequest request) {
        return responseFactory.success(managerAttendanceSessionService.checkOut(clubId, sessionId, request));
    }

    @PostMapping("/{sessionId}/bulk-check-in")
    public ApiResponse<ManagerAttendanceSessionDetailResponse> bulkCheckIn(@PathVariable("clubId") Long clubId,
                                                                           @PathVariable("sessionId") Long sessionId,
                                                                           @Valid @RequestBody ManagerAttendanceBulkMarkRequest request) {
        return responseFactory.success(managerAttendanceSessionService.bulkCheckIn(clubId, sessionId, request));
    }

    @PostMapping("/{sessionId}/bulk-check-out")
    public ApiResponse<ManagerAttendanceSessionDetailResponse> bulkCheckOut(@PathVariable("clubId") Long clubId,
                                                                            @PathVariable("sessionId") Long sessionId,
                                                                            @Valid @RequestBody ManagerAttendanceBulkMarkRequest request) {
        return responseFactory.success(managerAttendanceSessionService.bulkCheckOut(clubId, sessionId, request));
    }

    @PostMapping("/{sessionId}/settle")
    public ApiResponse<ManagerAttendanceSessionDetailResponse> settle(@PathVariable("clubId") Long clubId,
                                                                      @PathVariable("sessionId") Long sessionId) {
        return responseFactory.success(managerAttendanceSessionService.settle(clubId, sessionId));
    }
}
