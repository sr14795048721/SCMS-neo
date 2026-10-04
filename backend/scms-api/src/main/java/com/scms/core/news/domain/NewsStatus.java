package com.scms.core.news.domain;

public enum NewsStatus {
    DRAFT,
    PUBLISHED;

    public static NewsStatus fromValue(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        for (NewsStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("invalid news status");
    }
}
