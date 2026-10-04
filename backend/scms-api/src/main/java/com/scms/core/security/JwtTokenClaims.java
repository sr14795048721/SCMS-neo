package com.scms.core.security;

import com.scms.core.user.domain.UserRole;

import java.time.Instant;

public record JwtTokenClaims(
        Long userId,
        String username,
        UserRole role,
        String tokenId,
        String tokenType,
        Instant expiresAt
) {
}
