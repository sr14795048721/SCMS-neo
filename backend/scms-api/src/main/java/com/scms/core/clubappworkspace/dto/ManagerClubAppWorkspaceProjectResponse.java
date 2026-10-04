package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ManagerClubAppWorkspaceProjectResponse(
        Long id,
        String projectKey,
        String title,
        String subtitle,
        String summary,
        String coverUrl,
        Integer sortOrder,
        boolean enabled,
        List<ManagerClubAppWorkspaceProjectMaterialResponse> materials
) {
}
