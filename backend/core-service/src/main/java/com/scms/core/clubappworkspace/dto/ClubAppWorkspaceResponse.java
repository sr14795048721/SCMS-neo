package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ClubAppWorkspaceResponse(
        Long clubId,
        String clubName,
        List<ClubAppWorkspaceGroupResponse> groups
) {
}
