package com.scms.core.auth.dto;

public record LogoutRequest(
        String refreshToken
) {
}
