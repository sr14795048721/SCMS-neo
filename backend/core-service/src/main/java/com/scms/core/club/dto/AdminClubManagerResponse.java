package com.scms.core.club.dto;

public record AdminClubManagerResponse(
        Long userId,
        String username,
        String displayName,
        String managerNo,
        String phone
) {
}
