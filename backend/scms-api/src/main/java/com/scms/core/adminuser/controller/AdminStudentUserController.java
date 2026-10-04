package com.scms.core.adminuser.controller;

import com.scms.core.adminuser.dto.AdminBulkResetPasswordResponse;
import com.scms.core.adminuser.dto.AdminDeleteUserResponse;
import com.scms.core.adminuser.dto.AdminStudentDetailResponse;
import com.scms.core.adminuser.dto.AdminStudentImportResponse;
import com.scms.core.adminuser.dto.AdminStudentListItemResponse;
import com.scms.core.adminuser.dto.AdminStudentUpdateRequest;
import com.scms.core.adminuser.dto.AdminUserPageResponse;
import com.scms.core.adminuser.service.AdminUserExcelFileContent;
import com.scms.core.adminuser.service.AdminUserService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/admin/students")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminStudentUserController {

    private final AdminUserService adminUserService;
    private final ApiResponseFactory responseFactory;

    public AdminStudentUserController(AdminUserService adminUserService, ApiResponseFactory responseFactory) {
        this.adminUserService = adminUserService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<AdminUserPageResponse<AdminStudentListItemResponse>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        return responseFactory.success(adminUserService.listStudents(page, pageSize, keyword, sortBy, sortDirection));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminStudentDetailResponse> detail(@PathVariable("userId") Long userId) {
        return responseFactory.success(adminUserService.getStudentDetail(userId));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<AdminStudentDetailResponse> update(@PathVariable("userId") Long userId,
                                                          @Valid @RequestBody AdminStudentUpdateRequest request) {
        return responseFactory.success(adminUserService.updateStudent(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<AdminDeleteUserResponse> delete(@PathVariable("userId") Long userId) {
        return responseFactory.success(adminUserService.deleteStudent(userId));
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<AdminBulkResetPasswordResponse> resetPassword(@PathVariable("userId") Long userId) {
        return responseFactory.success(adminUserService.resetStudentPassword(userId));
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminStudentImportResponse> importStudents(@RequestPart("file") MultipartFile file) {
        return responseFactory.success(adminUserService.importStudents(file));
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(@RequestParam(name = "keyword", defaultValue = "") String keyword) {
        return buildFileResponse(adminUserService.exportStudents(keyword));
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template() {
        return buildFileResponse(adminUserService.downloadStudentTemplate());
    }

    @PostMapping("/reset-passwords")
    public ApiResponse<AdminBulkResetPasswordResponse> resetPasswords() {
        return responseFactory.success(adminUserService.resetStudentPasswords());
    }

    private ResponseEntity<ByteArrayResource> buildFileResponse(AdminUserExcelFileContent fileContent) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodeFileName(fileContent.fileName()))
                .contentType(MediaType.parseMediaType(fileContent.contentType()))
                .contentLength(fileContent.content().length)
                .body(new ByteArrayResource(fileContent.content()));
    }

    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
