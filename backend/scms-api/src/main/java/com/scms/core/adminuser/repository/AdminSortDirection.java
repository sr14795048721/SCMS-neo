package com.scms.core.adminuser.repository;

public enum AdminSortDirection {
    ASC,
    DESC;

    public static AdminSortDirection fromValue(String value) {
        if ("desc".equalsIgnoreCase(value)) {
            return DESC;
        }
        return ASC;
    }
}
