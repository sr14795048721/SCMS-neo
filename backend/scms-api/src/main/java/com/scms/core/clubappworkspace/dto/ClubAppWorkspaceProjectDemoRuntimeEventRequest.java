package com.scms.core.clubappworkspace.dto;

import java.time.Instant;

public record ClubAppWorkspaceProjectDemoRuntimeEventRequest(
        String eventType,
        String title,
        String detail,
        String level,
        Instant eventAt
) {
}
