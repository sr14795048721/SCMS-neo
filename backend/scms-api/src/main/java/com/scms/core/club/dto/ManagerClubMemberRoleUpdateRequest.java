package com.scms.core.club.dto;

import jakarta.validation.constraints.NotBlank;

public record ManagerClubMemberRoleUpdateRequest(
        @NotBlank(message = "role is required")
        String role
) {
}
