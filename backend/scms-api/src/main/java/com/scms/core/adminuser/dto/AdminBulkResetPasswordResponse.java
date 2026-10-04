package com.scms.core.adminuser.dto;

public record AdminBulkResetPasswordResponse(
        int resetCount,
        String defaultPassword
) {
}
