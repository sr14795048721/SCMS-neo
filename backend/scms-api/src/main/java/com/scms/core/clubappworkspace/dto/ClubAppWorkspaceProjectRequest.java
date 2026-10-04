package com.scms.core.clubappworkspace.dto;

public record ClubAppWorkspaceProjectRequest(
        Long id,
        String projectKey,
        String title,
        String subtitle,
        String summary,
        String coverUrl,
        Boolean enabled,
        java.util.List<ClubAppWorkspaceProjectMaterialRequest> materials
) {
}
