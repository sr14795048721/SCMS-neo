package com.scms.core.adminuser.dto;

import java.util.List;

public record AdminUserPageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        int totalPages
) {
}
