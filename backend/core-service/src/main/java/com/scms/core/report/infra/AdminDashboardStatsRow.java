package com.scms.core.report.infra;

public record AdminDashboardStatsRow(
        long totalUsers,
        long uniqueVisitors,
        long todayVisitCount,
        long onlineSessions
) {
}
