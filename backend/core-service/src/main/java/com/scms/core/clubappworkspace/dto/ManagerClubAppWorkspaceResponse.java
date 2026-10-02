package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ManagerClubAppWorkspaceResponse(
        Long clubId,
        String clubName,
        List<ManagerClubAppWorkspaceGroupResponse> groups
) {
}
