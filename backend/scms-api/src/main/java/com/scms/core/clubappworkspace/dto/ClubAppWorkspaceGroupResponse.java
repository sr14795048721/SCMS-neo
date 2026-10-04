package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ClubAppWorkspaceGroupResponse(
        Long id,
        String title,
        String subtitle,
        Integer sortOrder,
        List<ClubAppWorkspaceProjectResponse> projects
) {
}
