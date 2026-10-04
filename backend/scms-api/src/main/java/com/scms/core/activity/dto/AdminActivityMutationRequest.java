package com.scms.core.activity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AdminActivityMutationRequest(
        @NotNull(message = "clubId is required")
        Long clubId,
        @NotBlank(message = "title is required")
        @Size(max = 180, message = "title too long")
        String title,
        @Size(max = 1000, message = "description too long")
        String description,
        @Size(max = 160, message = "location too long")
        String location,
        @NotNull(message = "startTime is required")
        Instant startTime,
        @NotNull(message = "endTime is required")
        Instant endTime,
        @Min(value = 1, message = "capacity must be greater than 0")
        int capacity
) {
}
