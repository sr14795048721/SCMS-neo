package com.scms.core.adminuser.controller;

import com.scms.core.adminuser.dto.AdminBulkResetPasswordResponse;
import com.scms.core.adminuser.dto.AdminDeleteUserResponse;
import com.scms.core.adminuser.dto.AdminManagerCreateRequest;
import com.scms.core.adminuser.dto.AdminManagerCreateResponse;
import com.scms.core.adminuser.dto.AdminManagerDetailResponse;
import com.scms.core.adminuser.dto.AdminManagerListItemResponse;
import com.scms.core.adminuser.dto.AdminManagerOptionResponse;
import com.scms.core.adminuser.dto.AdminManagerUpdateRequest;
import com.scms.core.adminuser.dto.AdminUserPageResponse;
import com.scms.core.adminuser.service.AdminUserService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/managers")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminManagerUserController {

    private final AdminUserService adminUserService;
    private final ApiResponseFactory responseFactory;

    public AdminManagerUserController(AdminUserService adminUserService, ApiResponseFactory responseFactory) {
        this.adminUserService = adminUserService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<AdminUserPageResponse<AdminManagerListItemResponse>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        return responseFactory.success(adminUserService.listManagers(page, pageSize, keyword, sortBy, sortDirection));
    }

    @PostMapping
    public ApiResponse<AdminManagerCreateResponse> create(@Valid @RequestBody AdminManagerCreateRequest request) {
        return responseFactory.success(adminUserService.createManager(request));
    }

    @GetMapping("/options")
    public ApiResponse<List<AdminManagerOptionResponse>> options(
            @RequestParam(name = "keyword", defaultValue = "") String keyword
    ) {
        return responseFactory.success(adminUserService.listManagerOptions(keyword));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminManagerDetailResponse> detail(@PathVariable("userId") Long userId) {
        return responseFactory.success(adminUserService.getManagerDetail(userId));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<AdminManagerDetailResponse> update(@PathVariable("userId") Long userId,
                                                          @Valid @RequestBody AdminManagerUpdateRequest request) {
        return responseFactory.success(adminUserService.updateManager(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<AdminDeleteUserResponse> delete(@PathVariable("userId") Long userId) {
        return responseFactory.success(adminUserService.deleteManager(userId));
    }

    @PostMapping("/reset-passwords")
    public ApiResponse<AdminBulkResetPasswordResponse> resetPasswords() {
        return responseFactory.success(adminUserService.resetManagerPasswords());
    }
}
