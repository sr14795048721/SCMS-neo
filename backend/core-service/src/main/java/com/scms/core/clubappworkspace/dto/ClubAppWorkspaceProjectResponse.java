package com.scms.core.clubappworkspace.dto;

public record ClubAppWorkspaceProjectResponse(
        Long id,
        String projectKey,
        String title,
        String subtitle,
        String coverUrl,
        Integer sortOrder
) {
}
