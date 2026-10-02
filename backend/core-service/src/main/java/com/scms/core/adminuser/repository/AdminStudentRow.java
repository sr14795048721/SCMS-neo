package com.scms.core.adminuser.repository;

import java.time.Instant;

public record AdminStudentRow(
        Long userId,
        String username,
        String email,
        boolean enabled,
        String displayName,
        String studentNo,
        String grade,
        String className,
        String phone,
        String bio,
        boolean hasAvatar,
        Instant avatarUpdatedAt,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
