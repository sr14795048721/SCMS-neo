package com.scms.core.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateManagerInfoRequest(
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
