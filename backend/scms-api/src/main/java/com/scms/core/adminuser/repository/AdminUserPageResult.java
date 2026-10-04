package com.scms.core.adminuser.repository;

import java.util.List;

public record AdminUserPageResult<T>(
        List<T> items,
        long total
) {
}
