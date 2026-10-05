package com.scms.core.changelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record VersionChangelogMutationRequest(
        @NotBlank @Size(max = 64) String version,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @NotNull Instant releasedAt
) {
}
