package com.scms.core.admin.dto;

public record AdminDirtyDataCleanupResponse(
        int attendanceRecordsRemoved,
        int attendanceSessionsRemoved,
        int scoreRecordsRemoved,
        int rewardOrdersRemoved,
        int clubJoinRequestsRemoved,
        int registrationsRemoved,
        int clubMembersRemoved,
        int notificationsRemoved,
        int studentProfilesRemoved,
        int managerProfilesRemoved,
        int clubManagerBindingsRemoved,
        int clubDutiesRemoved,
        int rewardTargetClubsRemoved,
        int totalRemoved
) {
}
