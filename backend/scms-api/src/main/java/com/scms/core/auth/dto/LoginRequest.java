package com.scms.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "用户名或邮箱不能为空")
        String username,
        @NotBlank(message = "密码不能为空")
        String password
) {
}
