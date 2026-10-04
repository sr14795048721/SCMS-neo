package com.scms.core.adminuser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminManagerUpdateRequest(
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
        @Size(max = 64, message = "managerNo must be at most 64 characters")
        String managerNo,
        @Size(max = 32, message = "phone must be at most 32 characters")
        String phone,
        @Size(max = 1000, message = "bio must be at most 1000 characters")
        String bio
) {
}
