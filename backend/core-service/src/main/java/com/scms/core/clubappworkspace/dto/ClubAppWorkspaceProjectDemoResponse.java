package com.scms.core.clubappworkspace.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ClubAppWorkspaceProjectDemoResponse(
        Long projectId,
        String projectKey,
        String title,
        String subtitle,
        String coverUrl,
        String overviewTitle,
        String overviewBody,
        String bridgeMode,
        List<ClubAppWorkspaceProjectDemoStepResponse> steps,
        JsonNode mockSnapshot
) {
}
