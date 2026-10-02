package com.scms.core.clubappworkspace.dto;

import java.time.Instant;

public record ClubAppWorkspaceProjectDemoRuntimeEventResponse(
        Long id,
        String eventType,
        String title,
        String detail,
        String level,
        Instant eventAt
) {
}
