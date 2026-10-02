package com.scms.core.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStudentInfoRequest(
        @NotBlank(message = "displayName is required")
        @Size(max = 120, message = "displayName must be at most 120 characters")
        String displayName,
        @NotBlank(message = "studentNo is required")
        @Size(max = 64, message = "studentNo must be at most 64 characters")
        String studentNo,
        @NotBlank(message = "grade is required")
        @Pattern(regexp = "^(HIGH_1|HIGH_2|HIGH_3)$", message = "grade must be one of HIGH_1, HIGH_2, HIGH_3")
        String grade,
        @NotBlank(message = "className is required")
        @Pattern(regexp = "^$|^(?:[1-9]|[12][0-9]|30)$", message = "className must be empty or between 1 and 30")
        String className,
        @Size(max = 32, message = "phone must be at most 32 characters")
        String phone,
        @Size(max = 1000, message = "bio must be at most 1000 characters")
        String bio
) {
}
