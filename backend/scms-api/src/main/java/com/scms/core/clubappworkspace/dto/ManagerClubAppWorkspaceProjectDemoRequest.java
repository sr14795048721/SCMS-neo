package com.scms.core.clubappworkspace.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ManagerClubAppWorkspaceProjectDemoRequest(
        String overviewTitle,
        String overviewBody,
        String bridgeMode,
        Boolean enabled,
        List<ManagerClubAppWorkspaceProjectDemoStepRequest> steps,
        JsonNode mockSnapshot
) {
}
