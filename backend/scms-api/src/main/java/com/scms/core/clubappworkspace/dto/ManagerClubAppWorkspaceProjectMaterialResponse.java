package com.scms.core.clubappworkspace.dto;

public record ManagerClubAppWorkspaceProjectMaterialResponse(
        Long id,
        String sectionKey,
        String title,
        String storagePath,
        Integer sortOrder,
        boolean enabled
) {
}
