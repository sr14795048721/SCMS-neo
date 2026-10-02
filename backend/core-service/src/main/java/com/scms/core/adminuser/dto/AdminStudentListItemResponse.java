package com.scms.core.adminuser.dto;

import java.time.Instant;

public record AdminStudentListItemResponse(
        Long userId,
        String username,
        String email,
        boolean enabled,
        String displayName,
        String studentNo,
        String grade,
        String className,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
}
