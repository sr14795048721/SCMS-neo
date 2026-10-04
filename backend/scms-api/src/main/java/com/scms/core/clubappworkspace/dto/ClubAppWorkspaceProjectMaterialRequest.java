package com.scms.core.clubappworkspace.dto;

public record ClubAppWorkspaceProjectMaterialRequest(
        Long id,
        String sectionKey,
        String title,
        String storagePath,
        Boolean enabled
) {
}
