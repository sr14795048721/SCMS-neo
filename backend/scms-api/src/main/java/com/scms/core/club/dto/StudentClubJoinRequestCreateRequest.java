package com.scms.core.club.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentClubJoinRequestCreateRequest(
        @NotNull(message = "clubId is required")
        Long clubId,
        @Size(max = 500, message = "reason too long")
        String reason
) {
}
