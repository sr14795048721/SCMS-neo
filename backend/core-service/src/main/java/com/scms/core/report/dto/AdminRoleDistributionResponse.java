package com.scms.core.report.dto;

public record AdminRoleDistributionResponse(
        long studentCount,
        long managerCount,
        long adminCount
) {
}
