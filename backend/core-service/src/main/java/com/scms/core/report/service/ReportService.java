package com.scms.core.report.service;

import com.scms.core.report.dto.AdminDashboardStatsResponse;
import com.scms.core.report.dto.AdminStatisticsOverviewResponse;
import com.scms.core.report.dto.AdminStatisticsResponse;
import com.scms.core.report.dto.AdminClubRankingResponse;
import com.scms.core.report.dto.AdminRoleDistributionResponse;
import com.scms.core.report.dto.OverviewReportResponse;
import com.scms.core.report.infra.AdminDashboardStatsRow;
import com.scms.core.report.infra.AdminStatisticsOverviewRow;
import com.scms.core.report.infra.AdminRoleDistributionRow;
import com.scms.core.report.infra.OverviewReportRow;
import com.scms.core.report.infra.AdminClubRankingRow;
import com.scms.core.report.infra.ReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private static final int DEFAULT_ADMIN_CLUB_RANKING_LIMIT = 10;

    private final ReportMapper reportMapper;

    public ReportService(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Transactional(readOnly = true)
    public OverviewReportResponse overview() {
        OverviewReportRow row = reportMapper.fetchOverview();
        return new OverviewReportResponse(
                row.userCount(),
                row.clubCount(),
                row.activityCount(),
                row.activeRegistrationCount()
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse adminDashboard() {
        AdminDashboardStatsRow row = reportMapper.fetchAdminDashboardStats();
        return new AdminDashboardStatsResponse(
                row.totalUsers(),
                row.uniqueVisitors(),
                row.todayVisitCount(),
                row.onlineSessions()
        );
    }

    @Transactional(readOnly = true)
    public AdminStatisticsResponse adminStatistics() {
        AdminStatisticsOverviewRow overviewRow = reportMapper.fetchAdminStatisticsOverview();
        AdminRoleDistributionRow roleDistributionRow = reportMapper.fetchAdminRoleDistribution();
        List<AdminClubRankingRow> clubRankingRows = reportMapper.fetchAdminClubRankings(DEFAULT_ADMIN_CLUB_RANKING_LIMIT);

        AdminStatisticsOverviewResponse overview = new AdminStatisticsOverviewResponse(
                overviewRow == null ? 0L : overviewRow.totalUsers(),
                overviewRow == null ? 0L : overviewRow.activeUsers(),
                overviewRow == null ? 0L : overviewRow.totalClubs(),
                overviewRow == null ? 0L : overviewRow.totalActivities(),
                overviewRow == null ? 0L : overviewRow.activeRegistrations(),
                overviewRow == null ? 0L : overviewRow.uniqueVisitors(),
                overviewRow == null ? 0L : overviewRow.todayVisitCount(),
                overviewRow == null ? 0L : overviewRow.onlineSessions()
        );
        AdminRoleDistributionResponse roleDistribution = new AdminRoleDistributionResponse(
                roleDistributionRow == null ? 0L : roleDistributionRow.studentCount(),
                roleDistributionRow == null ? 0L : roleDistributionRow.managerCount(),
                roleDistributionRow == null ? 0L : roleDistributionRow.adminCount()
        );
        List<AdminClubRankingResponse> clubRankings = clubRankingRows == null
                ? List.of()
                : mapClubRankings(clubRankingRows);

        return new AdminStatisticsResponse(overview, roleDistribution, clubRankings);
    }

    private List<AdminClubRankingResponse> mapClubRankings(List<AdminClubRankingRow> rows) {
        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    AdminClubRankingRow row = rows.get(index);
                    return new AdminClubRankingResponse(
                            index + 1L,
                            row.clubId(),
                            row.clubName(),
                            row.memberCount(),
                            row.activityCount()
                    );
                })
                .toList();
    }
}
