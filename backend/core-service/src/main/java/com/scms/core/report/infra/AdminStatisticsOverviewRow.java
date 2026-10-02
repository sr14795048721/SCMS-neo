package com.scms.core.report.infra;

public record AdminStatisticsOverviewRow(
        long totalUsers,
        long activeUsers,
        long totalClubs,
        long totalActivities,
        long activeRegistrations,
        long uniqueVisitors,
        long todayVisitCount,
        long onlineSessions
) {
}
