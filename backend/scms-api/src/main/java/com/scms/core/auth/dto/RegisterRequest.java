package com.scms.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度需在3到64个字符之间")
        String username,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过128个字符")
        String email,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度需在8到72个字符之间")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "密码必须包含大小写字母、数字和特殊字符"
        )
        String password
) {
}
