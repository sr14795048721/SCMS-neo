package com.scms.core.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiCommitRequest(
        @NotBlank(message = "commit is required")
        String commit
) {
}
