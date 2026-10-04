package com.scms.core.clubappworkspace.dto;

public record ClubAppWorkspaceProjectDemoStepResponse(
        Long id,
        String title,
        String description,
        String triggerType,
        String targetSubsystem,
        String actionKey,
        Integer sortOrder
) {
}
