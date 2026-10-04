package com.scms.core.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicAttendanceSignRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 120, message = "姓名长度不能超过 120 个字符")
        String displayName,

        @NotBlank(message = "学号不能为空")
        @Size(max = 64, message = "学号长度不能超过 64 个字符")
        String studentNo,

        @NotBlank(message = "请完成手写签名后再提交")
        String signatureImage
) {
}
