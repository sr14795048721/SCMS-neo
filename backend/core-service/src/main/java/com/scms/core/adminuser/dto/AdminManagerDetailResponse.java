package com.scms.core.adminuser.dto;

import java.time.Instant;

public record AdminManagerDetailResponse(
        Long userId,
        String username,
        String email,
        boolean enabled,
        String displayName,
        String managerNo,
        String phone,
        String bio,
        String role,
        boolean hasAvatar,
        Instant avatarUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
