package com.scms.core.adminuser.repository;

import java.time.Instant;

public record AdminManagerRow(
        Long userId,
        String username,
        String email,
        boolean enabled,
        String displayName,
        String managerNo,
        String phone,
        String bio,
        boolean hasAvatar,
        Instant avatarUpdatedAt,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
