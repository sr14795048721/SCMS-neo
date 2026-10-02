package com.scms.core.auth.dto;

public record UserMeResponse(
        Long userId,
        String username,
        String role
) {
}
