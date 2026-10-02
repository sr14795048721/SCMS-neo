package com.scms.core.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds
) {
}
