package com.scms.core.clubappworkspace.dto;

public record ClubAppWorkspaceProjectMaterialResponse(
        Long id,
        String sectionKey,
        String title,
        String storagePath,
        String downloadUrl,
        Integer sortOrder
) {
}
