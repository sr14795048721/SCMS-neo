package com.scms.core.attendance.dto;

import java.util.List;

public record ManagerAttendanceSessionDetailResponse(
        ManagerAttendanceSessionSummaryResponse session,
        List<ManagerAttendanceSessionMemberResponse> members
) {
}
