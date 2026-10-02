package com.scms.core.adminuser.dto;

public record AdminManagerOptionResponse(
        Long userId,
        String username,
        String displayName,
        String managerNo
) {
}
