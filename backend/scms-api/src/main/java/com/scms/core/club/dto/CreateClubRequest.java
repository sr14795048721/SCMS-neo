package com.scms.core.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClubRequest(
        @NotBlank(message = "club name is required")
        @Size(max = 12, message = "club name too long")
        String name,
        @Size(max = 500, message = "description too long")
        String description
) {
}
