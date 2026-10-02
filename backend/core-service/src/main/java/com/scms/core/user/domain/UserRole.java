package com.scms.core.user.domain;

public enum UserRole {
    ADMIN,
    SUPER_ADMIN,
    CLUB_MANAGER,
    STUDENT;

    public boolean isSystemAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean isHiddenFromAdmin(UserRole viewerRole) {
        return this == SUPER_ADMIN && viewerRole == ADMIN;
    }
}
