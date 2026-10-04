package com.scms.core.adminuser.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserQueryRepositoryTest {

    @Test
    void buildStudentOrderByClauseShouldUseStableNameSort() {
        String clause = AdminUserQueryRepository.buildStudentOrderByClause(AdminUserSortBy.DISPLAY_NAME, AdminSortDirection.ASC);

        assertEquals("ORDER BY lower(COALESCE(si.display_name, '')) ASC, u.id DESC", clause);
    }

    @Test
    void buildStudentOrderByClauseShouldIncludeIdentityComponents() {
        String clause = AdminUserQueryRepository.buildStudentOrderByClause(AdminUserSortBy.IDENTITY, AdminSortDirection.DESC);

        assertTrue(clause.contains("CASE COALESCE(si.grade, '')"));
        assertTrue(clause.contains("CAST(COALESCE(NULLIF(si.class_name, ''), '999') AS integer) DESC"));
        assertTrue(clause.contains("lower(COALESCE(si.student_no, '')) DESC"));
        assertTrue(clause.endsWith("u.id DESC"));
    }

    @Test
    void buildManagerOrderByClauseShouldSortStatusWithDisabledFirstWhenDesc() {
        String clause = AdminUserQueryRepository.buildManagerOrderByClause(AdminUserSortBy.STATUS, AdminSortDirection.DESC);

        assertEquals("ORDER BY CASE WHEN u.enabled THEN 1 ELSE 0 END ASC, u.id DESC", clause);
    }

    @Test
    void buildManagerOrderByClauseShouldFallbackToCreatedAtWhenSortMissing() {
        String clause = AdminUserQueryRepository.buildManagerOrderByClause(null, AdminSortDirection.ASC);

        assertEquals("ORDER BY u.created_at DESC, u.id DESC", clause);
    }
}
