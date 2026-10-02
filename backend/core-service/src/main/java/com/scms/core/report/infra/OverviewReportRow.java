package com.scms.core.report.infra;

public record OverviewReportRow(
        long userCount,
        long clubCount,
        long activityCount,
        long activeRegistrationCount
) {
}
