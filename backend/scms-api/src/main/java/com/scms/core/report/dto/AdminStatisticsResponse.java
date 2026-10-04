package com.scms.core.report.dto;

import java.util.List;

public record AdminStatisticsResponse(
        AdminStatisticsOverviewResponse overview,
        AdminRoleDistributionResponse roleDistribution,
        List<AdminClubRankingResponse> clubRankings
) {
}
