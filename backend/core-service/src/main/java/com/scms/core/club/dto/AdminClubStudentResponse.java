package com.scms.core.club.dto;

public record AdminClubStudentResponse(
        Long userId,
        String username,
        String displayName,
        String studentNo,
        String grade,
        String className,
        Long dutyId,
        String dutyName,
        java.util.List<String> dutyPermissions
) {
}
