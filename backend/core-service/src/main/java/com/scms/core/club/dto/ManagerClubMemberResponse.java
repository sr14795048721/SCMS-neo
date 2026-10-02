package com.scms.core.club.dto;

import java.time.Instant;

public record ManagerClubMemberResponse(
        Long userId,
        String username,
        String displayName,
        String studentNo,
        String grade,
        String className,
        String role,
        Long dutyId,
        String dutyName,
        java.util.List<String> dutyPermissions,
        Instant joinedAt
) {
}
