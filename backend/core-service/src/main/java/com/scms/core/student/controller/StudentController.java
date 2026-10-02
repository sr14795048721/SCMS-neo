package com.scms.core.student.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.student.dto.ChangeStudentPasswordRequest;
import com.scms.core.student.dto.StudentInfoResponse;
import com.scms.core.student.dto.UpdateStudentInfoRequest;
import com.scms.core.student.service.StudentAvatarContent;
import com.scms.core.student.service.StudentService;
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
@RequestMapping("/api/v1/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;
    private final ApiResponseFactory responseFactory;

    public StudentController(StudentService studentService, ApiResponseFactory responseFactory) {
        this.studentService = studentService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/info")
    public ApiResponse<StudentInfoResponse> info() {
        return responseFactory.success(studentService.getCurrentInfo());
    }

    @PatchMapping("/info")
    public ApiResponse<StudentInfoResponse> updateInfo(@Valid @RequestBody UpdateStudentInfoRequest request) {
        return responseFactory.success(studentService.updateCurrentInfo(request));
    }

    @GetMapping("/avatar")
    public ResponseEntity<InputStreamResource> avatar() {
        StudentAvatarContent avatarContent = studentService.resolveCurrentAvatar();
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
    public ApiResponse<StudentInfoResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return responseFactory.success(studentService.uploadCurrentAvatar(file));
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangeStudentPasswordRequest request) {
        studentService.changeCurrentPassword(request);
        return responseFactory.success(null);
    }
}

