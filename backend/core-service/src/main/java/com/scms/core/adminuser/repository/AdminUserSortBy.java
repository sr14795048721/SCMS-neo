package com.scms.core.adminuser.repository;

public enum AdminUserSortBy {
    USERNAME,
    DISPLAY_NAME,
    IDENTITY,
    CONTACT,
    STATUS;

    public static AdminUserSortBy fromValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim()) {
            case "username" -> USERNAME;
            case "displayName" -> DISPLAY_NAME;
            case "identity" -> IDENTITY;
            case "contact" -> CONTACT;
            case "status" -> STATUS;
            default -> null;
        };
    }
}
