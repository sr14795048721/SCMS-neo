package com.scms.core.adminuser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminStudentUpdateRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 64, message = "username length must be between 3 and 64")
        String username,
        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        @Size(max = 128, message = "email must be at most 128 characters")
        String email,
        boolean enabled,
        @NotBlank(message = "displayName is required")
        @Size(max = 120, message = "displayName must be at most 120 characters")
        String displayName,
        @Size(max = 64, message = "studentNo must be at most 64 characters")
        String studentNo,
        @NotBlank(message = "grade is required")
        @Pattern(regexp = "^(HIGH_1|HIGH_2|HIGH_3)$", message = "grade must be one of HIGH_1, HIGH_2, HIGH_3")
        String grade,
        @Pattern(regexp = "^$|^(?:[1-9]|[12][0-9]|30)$", message = "className must be empty or between 1 and 30")
        String className,
        @Size(max = 32, message = "phone must be at most 32 characters")
        String phone,
        @Size(max = 1000, message = "bio must be at most 1000 characters")
        String bio
) {
}
