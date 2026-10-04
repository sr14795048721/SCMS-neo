package com.scms.core.manager.dto;

import java.time.Instant;

public record ManagerInfoResponse(
        Long userId,
        String username,
        String role,
        String email,
        String displayName,
        String managerNo,
        String phone,
        String bio,
        boolean hasAvatar,
        Instant avatarUpdatedAt
) {
}
