package com.scms.core.club.domain;

public enum ClubMemberRole {
    PRESIDENT(4, 0),
    VICE_PRESIDENT(3, 1),
    MINISTER(2, 2),
    SECRETARY(2, 3),
    TREASURER(2, 4),
    MEMBER(1, 5);

    private final int managementLevel;
    private final int displayOrder;

    ClubMemberRole(int managementLevel, int displayOrder) {
        this.managementLevel = managementLevel;
        this.displayOrder = displayOrder;
    }

    public int managementLevel() {
        return managementLevel;
    }

    public int displayOrder() {
        return displayOrder;
    }

    public boolean canManage(ClubMemberRole targetRole) {
        ClubMemberRole normalizedTargetRole = targetRole == null ? MEMBER : targetRole;
        return managementLevel > normalizedTargetRole.managementLevel;
    }
}
