package com.scms.core.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String requestId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>("OK", "success", data, requestId, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId, Instant.now());
    }
}
