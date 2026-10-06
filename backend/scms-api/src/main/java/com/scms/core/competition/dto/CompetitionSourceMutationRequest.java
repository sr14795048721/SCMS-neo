package com.scms.core.competition.dto;

import com.scms.core.competition.domain.CompetitionSourceEntity.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionSourceMutationRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String url,
        SourceType sourceType,
        @Size(max = 120) String wechatName,
        boolean enabled,
        boolean scanEnabled
) {
}
