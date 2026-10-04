package com.scms.core.report.infra;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM users WHERE role <> 'SUPER_ADMIN') AS user_count,
              (SELECT COUNT(*) FROM clubs) AS club_count,
              (SELECT COUNT(*) FROM activities) AS activity_count,
              (SELECT COUNT(*) FROM registrations WHERE status = 'REGISTERED') AS active_registration_count
            """)
    OverviewReportRow fetchOverview();

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM users WHERE enabled = TRUE AND role <> 'SUPER_ADMIN') AS total_users,
              (SELECT COUNT(DISTINCT visitor_id) FROM visit_events) AS unique_visitors,
              (
                SELECT COUNT(*)
                FROM visit_events
                WHERE timezone('Asia/Shanghai', visited_at)::date = timezone('Asia/Shanghai', now())::date
              ) AS today_visit_count,
              (
                SELECT COUNT(*)
                FROM visitor_sessions
                WHERE last_seen_at >= now() - INTERVAL '5 minutes'
              ) AS online_sessions
            """)
    AdminDashboardStatsRow fetchAdminDashboardStats();

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM users WHERE role <> 'SUPER_ADMIN') AS total_users,
              (SELECT COUNT(*) FROM users WHERE enabled = TRUE AND role <> 'SUPER_ADMIN') AS active_users,
              (SELECT COUNT(*) FROM clubs) AS total_clubs,
              (SELECT COUNT(*) FROM activities) AS total_activities,
              (SELECT COUNT(*) FROM registrations WHERE status = 'REGISTERED') AS active_registrations,
              (SELECT COUNT(DISTINCT visitor_id) FROM visit_events) AS unique_visitors,
              (
                SELECT COUNT(*)
                FROM visit_events
                WHERE timezone('Asia/Shanghai', visited_at)::date = timezone('Asia/Shanghai', now())::date
              ) AS today_visit_count,
              (
                SELECT COUNT(*)
                FROM visitor_sessions
                WHERE last_seen_at >= now() - INTERVAL '5 minutes'
              ) AS online_sessions
            """)
    AdminStatisticsOverviewRow fetchAdminStatisticsOverview();

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM users WHERE role = 'STUDENT') AS student_count,
              (SELECT COUNT(*) FROM users WHERE role = 'CLUB_MANAGER') AS manager_count,
              (SELECT COUNT(*) FROM users WHERE role = 'ADMIN') AS admin_count
            """)
    AdminRoleDistributionRow fetchAdminRoleDistribution();

    @Select("""
            SELECT
              c.id AS club_id,
              c.name AS club_name,
              COALESCE(member_stats.member_count, 0) AS member_count,
              COALESCE(activity_stats.activity_count, 0) AS activity_count
            FROM clubs c
            LEFT JOIN (
              SELECT club_id, COUNT(*) AS member_count
              FROM club_student_members
              GROUP BY club_id
            ) member_stats ON member_stats.club_id = c.id
            LEFT JOIN (
              SELECT club_id, COUNT(*) AS activity_count
              FROM activities
              GROUP BY club_id
            ) activity_stats ON activity_stats.club_id = c.id
            ORDER BY COALESCE(member_stats.member_count, 0) DESC,
                     COALESCE(activity_stats.activity_count, 0) DESC,
                     c.id ASC
            LIMIT #{limit}
            """)
    java.util.List<AdminClubRankingRow> fetchAdminClubRankings(@org.apache.ibatis.annotations.Param("limit") int limit);
}
