package com.scms.core.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClubCreationRequestRequest(
        @NotBlank
        @Size(max = 12, message = "club name too long")
        String name,
        @NotBlank
        @Size(max = 64)
        String type,
        @Size(max = 2000)
        String description,
        @NotBlank
        @Size(max = 2000)
        String applyReason
) {
}
