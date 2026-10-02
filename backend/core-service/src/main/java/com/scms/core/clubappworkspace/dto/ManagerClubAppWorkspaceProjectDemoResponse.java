package com.scms.core.clubappworkspace.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ManagerClubAppWorkspaceProjectDemoResponse(
        Long projectId,
        String projectKey,
        String title,
        String subtitle,
        String coverUrl,
        String overviewTitle,
        String overviewBody,
        String bridgeMode,
        boolean enabled,
        List<ManagerClubAppWorkspaceProjectDemoStepResponse> steps,
        JsonNode mockSnapshot
) {
}
