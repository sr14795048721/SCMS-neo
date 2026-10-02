package com.scms.core.clubappworkspace.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ClubAppWorkspaceProjectDemoRiskSyncRequest(
        Long clubId,
        String projectKey,
        String sessionId,
        String sourceType,
        JsonNode riskDemo,
        List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> events
) {
}
