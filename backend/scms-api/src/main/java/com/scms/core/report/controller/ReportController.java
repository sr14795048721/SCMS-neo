package com.scms.core.report.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.report.dto.AdminDashboardStatsResponse;
import com.scms.core.report.dto.AdminStatisticsResponse;
import com.scms.core.report.dto.OverviewReportResponse;
import com.scms.core.report.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final ApiResponseFactory responseFactory;

    public ReportController(ReportService reportService, ApiResponseFactory responseFactory) {
        this.reportService = reportService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CLUB_MANAGER')")
    public ApiResponse<OverviewReportResponse> overview() {
        return responseFactory.success(reportService.overview());
    }

    @GetMapping("/admin-dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminDashboardStatsResponse> adminDashboard() {
        return responseFactory.success(reportService.adminDashboard());
    }

    @GetMapping("/admin-statistics")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminStatisticsResponse> adminStatistics() {
        return responseFactory.success(reportService.adminStatistics());
    }
}
