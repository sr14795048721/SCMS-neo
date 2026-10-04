package com.scms.core.student.dto;

import java.time.Instant;
import java.util.List;

public record StudentInfoResponse(
        Long userId,
        String username,
        String role,
        String email,
        String displayName,
        String studentNo,
        String grade,
        String className,
        String phone,
        String bio,
        boolean hasAvatar,
        Instant avatarUpdatedAt,
        boolean profileCompleted,
        List<String> missingRequiredFields
) {
}
