package com.scms.core.security;

import com.scms.core.user.domain.UserRole;

public record AuthenticatedUser(
        Long userId,
        String username,
        UserRole role
) {
}
