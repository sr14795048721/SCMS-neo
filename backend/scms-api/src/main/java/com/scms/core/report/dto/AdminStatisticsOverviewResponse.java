package com.scms.core.report.dto;

public record AdminStatisticsOverviewResponse(
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
