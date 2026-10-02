package com.scms.core.report.infra;

public record AdminRoleDistributionRow(
        long studentCount,
        long managerCount,
        long adminCount
) {
}
