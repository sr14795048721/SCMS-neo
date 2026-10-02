package com.scms.core.common.web;

import com.scms.core.common.api.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    public <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(data, RequestIdUtil.resolveFromMdcOrGenerate());
    }
}
