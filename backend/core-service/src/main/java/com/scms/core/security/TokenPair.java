package com.scms.core.security;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds
) {
}
