package com.scms.core.manager.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.manager.dto.ChangeManagerPasswordRequest;
import com.scms.core.manager.dto.ManagerInfoResponse;
import com.scms.core.manager.dto.UpdateManagerInfoRequest;
import com.scms.core.manager.service.ManagerAvatarContent;
import com.scms.core.manager.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/managers/me")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerController {

    private final ManagerService managerService;
    private final ApiResponseFactory responseFactory;

    public ManagerController(ManagerService managerService, ApiResponseFactory responseFactory) {
        this.managerService = managerService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/info")
    public ApiResponse<ManagerInfoResponse> info() {
        return responseFactory.success(managerService.getCurrentInfo());
    }

    @PatchMapping("/info")
    public ApiResponse<ManagerInfoResponse> updateInfo(@Valid @RequestBody UpdateManagerInfoRequest request) {
        return responseFactory.success(managerService.updateCurrentInfo(request));
    }

    @GetMapping("/avatar")
    public ResponseEntity<InputStreamResource> avatar() {
        ManagerAvatarContent avatarContent = managerService.resolveCurrentAvatar();
        try {
            InputStream inputStream = Files.newInputStream(avatarContent.path());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .contentType(MediaType.parseMediaType(avatarContent.contentType()))
                    .contentLength(avatarContent.contentLength())
                    .body(new InputStreamResource(inputStream));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ManagerInfoResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return responseFactory.success(managerService.uploadCurrentAvatar(file));
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangeManagerPasswordRequest request) {
        managerService.changeCurrentPassword(request);
        return responseFactory.success(null);
    }
}
