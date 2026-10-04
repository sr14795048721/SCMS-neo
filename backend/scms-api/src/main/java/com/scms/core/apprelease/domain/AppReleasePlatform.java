package com.scms.core.apprelease.domain;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;

public enum AppReleasePlatform {
    ANDROID,
    HARMONY;

    public static AppReleasePlatform fromValue(String value) {
        for (AppReleasePlatform platform : values()) {
            if (platform.name().equalsIgnoreCase(value)) {
                return platform;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid app release platform");
    }
}
