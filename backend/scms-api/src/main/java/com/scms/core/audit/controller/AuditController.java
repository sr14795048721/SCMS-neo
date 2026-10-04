package com.scms.core.audit.controller;

import com.scms.core.audit.dto.AuditLogResponse;
import com.scms.core.audit.service.AuditService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    private final AuditService auditService;
    private final ApiResponseFactory responseFactory;

    public AuditController(AuditService auditService, ApiResponseFactory responseFactory) {
        this.auditService = auditService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<AuditLogResponse>> latest() {
        return responseFactory.success(auditService.latest());
    }
}
