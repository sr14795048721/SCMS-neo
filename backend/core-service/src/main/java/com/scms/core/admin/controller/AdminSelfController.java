package com.scms.core.admin.controller;

import com.scms.core.admin.dto.ChangeAdminPasswordRequest;
import com.scms.core.admin.dto.AdminDirtyDataCleanupResponse;
import com.scms.core.admin.service.AdminSelfService;
import com.scms.core.admin.service.AdminSystemMaintenanceService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/me")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminSelfController {

    private final AdminSelfService adminSelfService;
    private final AdminSystemMaintenanceService adminSystemMaintenanceService;
    private final ApiResponseFactory responseFactory;

    public AdminSelfController(AdminSelfService adminSelfService,
                               AdminSystemMaintenanceService adminSystemMaintenanceService,
                               ApiResponseFactory responseFactory) {
        this.adminSelfService = adminSelfService;
        this.adminSystemMaintenanceService = adminSystemMaintenanceService;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangeAdminPasswordRequest request) {
        adminSelfService.changeCurrentPassword(request);
        return responseFactory.success(null);
    }

    @DeleteMapping("/dirty-data")
    public ApiResponse<AdminDirtyDataCleanupResponse> cleanDirtyData() {
        return responseFactory.success(adminSystemMaintenanceService.cleanDirtyData());
    }
}
