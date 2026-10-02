package com.scms.core.homebanner.domain;

public enum HomeBannerMediaType {
    IMAGE,
    VIDEO;

    public static HomeBannerMediaType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("media type is required");
        }
        return HomeBannerMediaType.valueOf(value.trim().toUpperCase());
    }

    public String toApiValue() {
        return name().toLowerCase();
    }
}
