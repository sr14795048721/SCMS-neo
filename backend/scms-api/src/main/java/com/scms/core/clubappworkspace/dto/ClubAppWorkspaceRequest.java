package com.scms.core.clubappworkspace.dto;

import java.util.List;

public record ClubAppWorkspaceRequest(
        List<ClubAppWorkspaceGroupRequest> groups
) {
}
