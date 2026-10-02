package com.scms.core.club.dto;

import java.util.List;

public record StudentDiscoverClubsResponse(
        StudentDiscoverClubResponse joinedClub,
        StudentPendingClubJoinRequestResponse pendingRequest,
        List<StudentDiscoverClubResponse> clubs
) {
}
