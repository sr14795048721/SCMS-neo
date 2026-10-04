package com.scms.core.adminuser.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminUserQueryRepository {

    private static final String STUDENT_BASE_SELECT = """
            FROM users u
            LEFT JOIN student_info si ON si.user_id = u.id
            WHERE u.role = :role
              AND (
                :keyword = ''
                OR lower(u.username) LIKE :keywordLike
                OR lower(u.email) LIKE :keywordLike
                OR lower(COALESCE(si.display_name, '')) LIKE :keywordLike
                OR lower(COALESCE(si.student_no, '')) LIKE :keywordLike
                OR lower(COALESCE(si.phone, '')) LIKE :keywordLike
              )
            """;

    private static final String MANAGER_BASE_SELECT = """
            FROM users u
            LEFT JOIN manager_info mi ON mi.user_id = u.id
            WHERE u.role = :role
              AND (
                :keyword = ''
                OR lower(u.username) LIKE :keywordLike
                OR lower(u.email) LIKE :keywordLike
                OR lower(COALESCE(mi.display_name, '')) LIKE :keywordLike
                OR lower(COALESCE(mi.manager_no, '')) LIKE :keywordLike
                OR lower(COALESCE(mi.phone, '')) LIKE :keywordLike
              )
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminUserPageResult<AdminStudentRow> findStudents(String role, String keyword, int page, int pageSize) {
        return findStudents(role, keyword, page, pageSize, null, AdminSortDirection.ASC);
    }

    public AdminUserPageResult<AdminStudentRow> findStudents(String role,
                                                             String keyword,
                                                             int page,
                                                             int pageSize,
                                                             AdminUserSortBy sortBy,
                                                             AdminSortDirection sortDirection) {
        String select = """
                SELECT u.id,
                       u.username,
                       u.email,
                       u.enabled,
                       COALESCE(si.display_name, ''),
                       COALESCE(si.student_no, ''),
                       COALESCE(si.grade, ''),
                       COALESCE(si.class_name, ''),
                       COALESCE(si.phone, ''),
                       COALESCE(si.bio, ''),
                       CASE WHEN si.avatar_path IS NULL OR btrim(si.avatar_path) = '' THEN false ELSE true END,
                       si.avatar_updated_at,
                       u.role,
                       u.created_at,
                       u.updated_at
                """ + STUDENT_BASE_SELECT + """
                 """ + buildStudentOrderByClause(sortBy, sortDirection)
                + """
                """;

        Query query = entityManager.createNativeQuery(select);
        applyKeyword(query, role, keyword);
        query.setFirstResult(Math.max(page - 1, 0) * pageSize);
        query.setMaxResults(pageSize);

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + STUDENT_BASE_SELECT);
        applyKeyword(countQuery, role, keyword);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return new AdminUserPageResult<>(rows.stream().map(this::mapStudentRow).toList(), toLong(countQuery.getSingleResult()));
    }

    public List<AdminStudentRow> findStudentsForExport(String role, String keyword) {
        String select = """
                SELECT u.id,
                       u.username,
                       u.email,
                       u.enabled,
                       COALESCE(si.display_name, ''),
                       COALESCE(si.student_no, ''),
                       COALESCE(si.grade, ''),
                       COALESCE(si.class_name, ''),
                       COALESCE(si.phone, ''),
                       COALESCE(si.bio, ''),
                       CASE WHEN si.avatar_path IS NULL OR btrim(si.avatar_path) = '' THEN false ELSE true END,
                       si.avatar_updated_at,
                       u.role,
                       u.created_at,
                       u.updated_at
                """ + STUDENT_BASE_SELECT + """
                 ORDER BY u.created_at DESC, u.id DESC
                """;

        Query query = entityManager.createNativeQuery(select);
        applyKeyword(query, role, keyword);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::mapStudentRow).toList();
    }

    public Optional<AdminStudentRow> findStudentByUserId(String role, Long userId) {
        String select = """
                SELECT u.id,
                       u.username,
                       u.email,
                       u.enabled,
                       COALESCE(si.display_name, ''),
                       COALESCE(si.student_no, ''),
                       COALESCE(si.grade, ''),
                       COALESCE(si.class_name, ''),
                       COALESCE(si.phone, ''),
                       COALESCE(si.bio, ''),
                       CASE WHEN si.avatar_path IS NULL OR btrim(si.avatar_path) = '' THEN false ELSE true END,
                       si.avatar_updated_at,
                       u.role,
                       u.created_at,
                       u.updated_at
                  FROM users u
                  LEFT JOIN student_info si ON si.user_id = u.id
                 WHERE u.role = :role
                   AND u.id = :userId
                """;

        Query query = entityManager.createNativeQuery(select);
        query.setParameter("role", role);
        query.setParameter("userId", userId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().findFirst().map(this::mapStudentRow);
    }

    public AdminUserPageResult<AdminManagerRow> findManagers(String role, String keyword, int page, int pageSize) {
        return findManagers(role, keyword, page, pageSize, null, AdminSortDirection.ASC);
    }

    public AdminUserPageResult<AdminManagerRow> findManagers(String role,
                                                             String keyword,
                                                             int page,
                                                             int pageSize,
                                                             AdminUserSortBy sortBy,
                                                             AdminSortDirection sortDirection) {
        String select = """
                SELECT u.id,
                       u.username,
                       u.email,
                       u.enabled,
                       COALESCE(mi.display_name, ''),
                       COALESCE(mi.manager_no, ''),
                       COALESCE(mi.phone, ''),
                       COALESCE(mi.bio, ''),
                       CASE WHEN mi.avatar_path IS NULL OR btrim(mi.avatar_path) = '' THEN false ELSE true END,
                       mi.avatar_updated_at,
                       u.role,
                       u.created_at,
                       u.updated_at
                """ + MANAGER_BASE_SELECT + """
                 """ + buildManagerOrderByClause(sortBy, sortDirection)
                + """
                """;

        Query query = entityManager.createNativeQuery(select);
        applyKeyword(query, role, keyword);
        query.setFirstResult(Math.max(page - 1, 0) * pageSize);
        query.setMaxResults(pageSize);

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + MANAGER_BASE_SELECT);
        applyKeyword(countQuery, role, keyword);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return new AdminUserPageResult<>(rows.stream().map(this::mapManagerRow).toList(), toLong(countQuery.getSingleResult()));
    }

    public Optional<AdminManagerRow> findManagerByUserId(String role, Long userId) {
        String select = """
                SELECT u.id,
                       u.username,
                       u.email,
                       u.enabled,
                       COALESCE(mi.display_name, ''),
                       COALESCE(mi.manager_no, ''),
                       COALESCE(mi.phone, ''),
                       COALESCE(mi.bio, ''),
                       CASE WHEN mi.avatar_path IS NULL OR btrim(mi.avatar_path) = '' THEN false ELSE true END,
                       mi.avatar_updated_at,
                       u.role,
                       u.created_at,
                       u.updated_at
                  FROM users u
                  LEFT JOIN manager_info mi ON mi.user_id = u.id
                 WHERE u.role = :role
                   AND u.id = :userId
                """;

        Query query = entityManager.createNativeQuery(select);
        query.setParameter("role", role);
        query.setParameter("userId", userId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().findFirst().map(this::mapManagerRow);
    }

    private void applyKeyword(Query query, String role, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        query.setParameter("role", role);
        query.setParameter("keyword", normalizedKeyword);
        query.setParameter("keywordLike", normalizedKeyword.isEmpty() ? "%" : "%" + normalizedKeyword + "%");
    }

    static String buildStudentOrderByClause(AdminUserSortBy sortBy, AdminSortDirection sortDirection) {
        if (sortBy == null) {
            return "ORDER BY u.created_at DESC, u.id DESC";
        }
        return switch (sortBy) {
            case USERNAME -> "ORDER BY lower(u.username) " + sqlDirection(sortDirection) + ", u.id DESC";
            case DISPLAY_NAME -> "ORDER BY lower(COALESCE(si.display_name, '')) " + sqlDirection(sortDirection) + ", u.id DESC";
            case IDENTITY -> """
                    ORDER BY
                      CASE COALESCE(si.grade, '')
                        WHEN 'HIGH_1' THEN 1
                        WHEN 'HIGH_2' THEN 2
                        WHEN 'HIGH_3' THEN 3
                        ELSE 9
                      END %s,
                      CAST(COALESCE(NULLIF(si.class_name, ''), '999') AS integer) %s,
                      lower(COALESCE(si.student_no, '')) %s,
                      u.id DESC
                    """.formatted(sqlDirection(sortDirection), sqlDirection(sortDirection), sqlDirection(sortDirection)).trim();
            case CONTACT -> "ORDER BY lower(COALESCE(si.phone, '')) " + sqlDirection(sortDirection) + ", u.id DESC";
            case STATUS -> "ORDER BY " + buildStatusOrderExpression(sortDirection) + ", u.id DESC";
        };
    }

    static String buildManagerOrderByClause(AdminUserSortBy sortBy, AdminSortDirection sortDirection) {
        if (sortBy == null) {
            return "ORDER BY u.created_at DESC, u.id DESC";
        }
        return switch (sortBy) {
            case USERNAME -> "ORDER BY lower(u.username) " + sqlDirection(sortDirection) + ", u.id DESC";
            case DISPLAY_NAME -> "ORDER BY lower(COALESCE(mi.display_name, '')) " + sqlDirection(sortDirection) + ", u.id DESC";
            case IDENTITY -> "ORDER BY lower(COALESCE(mi.manager_no, '')) " + sqlDirection(sortDirection) + ", u.id DESC";
            case CONTACT -> "ORDER BY lower(COALESCE(mi.phone, '')) " + sqlDirection(sortDirection) + ", u.id DESC";
            case STATUS -> "ORDER BY " + buildStatusOrderExpression(sortDirection) + ", u.id DESC";
        };
    }

    private static String sqlDirection(AdminSortDirection sortDirection) {
        return sortDirection == AdminSortDirection.DESC ? "DESC" : "ASC";
    }

    private static String buildStatusOrderExpression(AdminSortDirection sortDirection) {
        if (sortDirection == AdminSortDirection.DESC) {
            return "CASE WHEN u.enabled THEN 1 ELSE 0 END ASC";
        }
        return "CASE WHEN u.enabled THEN 0 ELSE 1 END ASC";
    }

    private AdminStudentRow mapStudentRow(Object[] row) {
        return new AdminStudentRow(
                toLong(row[0]),
                toStringValue(row[1]),
                toStringValue(row[2]),
                toBoolean(row[3]),
                toStringValue(row[4]),
                toStringValue(row[5]),
                toStringValue(row[6]),
                toStringValue(row[7]),
                toStringValue(row[8]),
                toStringValue(row[9]),
                toBoolean(row[10]),
                toInstant(row[11]),
                toStringValue(row[12]),
                toInstant(row[13]),
                toInstant(row[14])
        );
    }

    private AdminManagerRow mapManagerRow(Object[] row) {
        return new AdminManagerRow(
                toLong(row[0]),
                toStringValue(row[1]),
                toStringValue(row[2]),
                toBoolean(row[3]),
                toStringValue(row[4]),
                toStringValue(row[5]),
                toStringValue(row[6]),
                toStringValue(row[7]),
                toBoolean(row[8]),
                toInstant(row[9]),
                toStringValue(row[10]),
                toInstant(row[11]),
                toInstant(row[12])
        );
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        return Instant.parse(String.valueOf(value));
    }
}
