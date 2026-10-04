package com.scms.core.adminuser.dto;

import java.time.Instant;

public record AdminManagerListItemResponse(
        Long userId,
        String username,
        String email,
        boolean enabled,
        String displayName,
        String managerNo,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
}
