package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ManagerClubAppWorkspaceGroupResponse(
        Long id,
        String title,
        String subtitle,
        Integer sortOrder,
        boolean enabled,
        List<ManagerClubAppWorkspaceProjectResponse> projects
) {
}
