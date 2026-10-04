package com.scms.core.report.dto;

public record OverviewReportResponse(
        long userCount,
        long clubCount,
        long activityCount,
        long activeRegistrationCount
) {
}
