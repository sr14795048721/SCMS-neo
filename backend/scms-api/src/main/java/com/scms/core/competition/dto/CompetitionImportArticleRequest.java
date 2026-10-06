package com.scms.core.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionImportArticleRequest(
        @NotBlank @Size(max = 500) String url,
        @Size(max = 300) String title,
        String content
) {
}
