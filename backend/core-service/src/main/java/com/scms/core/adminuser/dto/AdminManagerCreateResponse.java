package com.scms.core.adminuser.dto;

public record AdminManagerCreateResponse(
        AdminManagerDetailResponse manager,
        String defaultPassword
) {
}
