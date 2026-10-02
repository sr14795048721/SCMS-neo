package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ClubAppWorkspaceGroupRequest(
        Long id,
        String title,
        String subtitle,
        Boolean enabled,
        List<ClubAppWorkspaceProjectRequest> projects
) {
}
