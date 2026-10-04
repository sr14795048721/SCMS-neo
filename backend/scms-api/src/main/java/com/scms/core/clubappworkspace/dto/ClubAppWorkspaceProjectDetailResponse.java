package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ClubAppWorkspaceProjectDetailResponse(
        Long id,
        String projectKey,
        String title,
        String subtitle,
        String coverUrl,
        String summary,
        List<ClubAppWorkspaceProjectMaterialResponse> materials,
        String demoMode
) {
}
