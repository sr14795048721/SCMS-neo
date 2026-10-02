package com.scms.core.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminClubMutationRequest(
        @NotBlank(message = "club name is required")
        @Size(max = 12, message = "club name too long")
        String name,
        @NotBlank(message = "club type is required")
        @Size(max = 64, message = "club type too long")
        String type,
        @NotBlank(message = "club status is required")
        String status,
        @Size(max = 1000, message = "description too long")
        String description,
        List<@NotNull(message = "manager user id is required") Long> managerUserIds
) {
}
