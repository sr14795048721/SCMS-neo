package com.scms.core.clubappworkspace.dto;

public record ManagerClubAppWorkspaceProjectDemoStepRequest(
        Long id,
        String title,
        String description,
        String triggerType,
        String targetSubsystem,
        String actionKey,
        Boolean enabled
) {
}
