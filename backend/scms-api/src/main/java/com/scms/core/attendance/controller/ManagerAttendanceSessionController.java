package com.scms.core.attendance.controller;

import com.scms.core.attendance.dto.ManagerAttendanceSessionCreateRequest;
import com.scms.core.attendance.dto.ManagerAttendanceSessionDetailResponse;
import com.scms.core.attendance.dto.ManagerAttendanceSessionSummaryResponse;
import com.scms.core.attendance.service.ManagerAttendanceSessionService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
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
import java.nio.file.Path;
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

    @PostMapping("/{sessionId}/settle")
    public ApiResponse<ManagerAttendanceSessionDetailResponse> settle(@PathVariable("clubId") Long clubId,
                                                                      @PathVariable("sessionId") Long sessionId) {
        return responseFactory.success(managerAttendanceSessionService.settle(clubId, sessionId));
    }

    @GetMapping("/{sessionId}/records/{studentUserId}/signature")
    public ResponseEntity<InputStreamResource> signature(@PathVariable("clubId") Long clubId,
                                                         @PathVariable("sessionId") Long sessionId,
                                                         @PathVariable("studentUserId") Long studentUserId) {
        Path signaturePath = managerAttendanceSessionService.resolveSignature(clubId, sessionId, studentUserId);
        return buildBinaryResponse(signaturePath);
    }

    private ResponseEntity<InputStreamResource> buildBinaryResponse(Path path) {
        try {
            InputStream inputStream = Files.newInputStream(path);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .contentType(MediaType.IMAGE_PNG)
                    .contentLength(Files.size(path))
                    .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
