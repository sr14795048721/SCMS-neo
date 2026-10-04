package com.scms.core.clubappworkspace.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ClubAppWorkspaceProjectDemoRuntimeRequest(
        String sessionId,
        String sourceType,
        JsonNode watch,
        JsonNode helmet,
        JsonNode app,
        JsonNode alerts,
        JsonNode recommendation,
        List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> events
) {
}
