package com.scms.core.report;

import com.scms.core.report.dto.AdminDashboardStatsResponse;
import com.scms.core.report.dto.AdminStatisticsResponse;
import com.scms.core.report.infra.AdminClubRankingRow;
import com.scms.core.report.infra.AdminDashboardStatsRow;
import com.scms.core.report.infra.AdminRoleDistributionRow;
import com.scms.core.report.infra.AdminStatisticsOverviewRow;
import com.scms.core.report.infra.OverviewReportRow;
import com.scms.core.report.infra.ReportMapper;
import com.scms.core.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private ReportMapper reportMapper;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportMapper = mock(ReportMapper.class);
        reportService = new ReportService(reportMapper);
    }

    @Test
    void overviewShouldMapOverviewRow() {
        when(reportMapper.fetchOverview()).thenReturn(new OverviewReportRow(10, 4, 8, 3));

        var response = reportService.overview();

        assertEquals(10, response.userCount());
        assertEquals(4, response.clubCount());
        assertEquals(8, response.activityCount());
        assertEquals(3, response.activeRegistrationCount());
    }

    @Test
    void adminDashboardShouldMapTelemetryStats() {
        when(reportMapper.fetchAdminDashboardStats()).thenReturn(new AdminDashboardStatsRow(21, 14, 7, 5));

        AdminDashboardStatsResponse response = reportService.adminDashboard();

        assertEquals(21, response.totalUsers());
        assertEquals(14, response.uniqueVisitors());
        assertEquals(7, response.todayVisitCount());
        assertEquals(5, response.onlineSessions());
    }

    @Test
    void adminStatisticsShouldMapAggregatedRows() {
        when(reportMapper.fetchAdminStatisticsOverview()).thenReturn(new AdminStatisticsOverviewRow(30, 24, 6, 12, 9, 18, 4, 3));
        when(reportMapper.fetchAdminRoleDistribution()).thenReturn(new AdminRoleDistributionRow(20, 7, 3));
        when(reportMapper.fetchAdminClubRankings(10)).thenReturn(List.of(
                new AdminClubRankingRow(5L, "Robotics Club", 42L, 8L),
                new AdminClubRankingRow(2L, "Music Club", 35L, 6L)
        ));

        AdminStatisticsResponse response = reportService.adminStatistics();

        assertEquals(30, response.overview().totalUsers());
        assertEquals(24, response.overview().activeUsers());
        assertEquals(6, response.overview().totalClubs());
        assertEquals(12, response.overview().totalActivities());
        assertEquals(9, response.overview().activeRegistrations());
        assertEquals(18, response.overview().uniqueVisitors());
        assertEquals(4, response.overview().todayVisitCount());
        assertEquals(3, response.overview().onlineSessions());
        assertEquals(20, response.roleDistribution().studentCount());
        assertEquals(7, response.roleDistribution().managerCount());
        assertEquals(3, response.roleDistribution().adminCount());
        assertEquals(2, response.clubRankings().size());
        assertEquals(1, response.clubRankings().get(0).rank());
        assertEquals("Robotics Club", response.clubRankings().get(0).clubName());
        assertEquals(2, response.clubRankings().get(1).rank());
    }

    @Test
    void adminStatisticsShouldReturnEmptyRankingListWhenMapperReturnsNull() {
        when(reportMapper.fetchAdminStatisticsOverview()).thenReturn(null);
        when(reportMapper.fetchAdminRoleDistribution()).thenReturn(null);
        when(reportMapper.fetchAdminClubRankings(10)).thenReturn(null);

        AdminStatisticsResponse response = reportService.adminStatistics();

        assertEquals(0, response.overview().totalUsers());
        assertEquals(0, response.roleDistribution().studentCount());
        assertEquals(0, response.clubRankings().size());
    }
}
