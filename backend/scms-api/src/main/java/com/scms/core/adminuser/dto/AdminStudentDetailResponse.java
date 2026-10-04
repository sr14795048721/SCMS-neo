package com.scms.core.adminuser.dto;

import java.time.Instant;

public record AdminStudentDetailResponse(
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
        String role,
        boolean hasAvatar,
        Instant avatarUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
