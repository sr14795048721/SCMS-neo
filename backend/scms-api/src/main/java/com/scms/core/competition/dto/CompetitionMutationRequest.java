package com.scms.core.competition.dto;

import com.scms.core.competition.domain.CompetitionEntity.CompetitionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CompetitionMutationRequest(
        @NotBlank @Size(max = 300) String name,
        @Size(max = 120) String category,
        @Size(max = 200) String participantScope,
        Instant applyDeadline,
        Instant startDate,
        Instant endDate,
        @Size(max = 500) String url,
        Long sourceId,
        CompetitionStatus status,
        String note
) {
}
