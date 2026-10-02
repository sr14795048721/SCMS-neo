package com.scms.core.clubappworkspace.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record ClubAppWorkspaceProjectDemoRuntimeResponse(
        String projectKey,
        String sessionId,
        JsonNode watch,
        JsonNode helmet,
        JsonNode app,
        JsonNode alerts,
        JsonNode recommendation,
        List<ClubAppWorkspaceProjectDemoRuntimeEventResponse> events,
        Instant updatedAt
) {
}
