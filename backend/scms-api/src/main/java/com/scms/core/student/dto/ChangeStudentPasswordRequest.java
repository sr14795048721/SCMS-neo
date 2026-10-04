package com.scms.core.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeStudentPasswordRequest(
        @NotBlank(message = "currentPassword is required")
        String currentPassword,
        @NotBlank(message = "newPassword is required")
        @Size(min = 6, max = 72, message = "newPassword must be between 6 and 72 characters")
        String newPassword,
        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}

