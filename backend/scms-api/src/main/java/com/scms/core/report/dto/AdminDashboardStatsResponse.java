package com.scms.core.report.dto;

public record AdminDashboardStatsResponse(
        long totalUsers,
        long uniqueVisitors,
        long todayVisitCount,
        long onlineSessions
) {
}
