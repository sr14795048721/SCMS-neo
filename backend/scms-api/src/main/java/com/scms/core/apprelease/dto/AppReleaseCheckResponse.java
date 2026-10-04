package com.scms.core.apprelease.dto;

import java.time.Instant;

public record AppReleaseCheckResponse(
        boolean hasUpdate,
        boolean forceUpdate,
        Long releaseId,
        String versionName,
        Integer buildNumber,
        String releaseNotes,
        String downloadUrl,
        Instant publishedAt
) {
    public static AppReleaseCheckResponse noUpdate() {
        return new AppReleaseCheckResponse(false, false, null, "", 0, "", "", null);
    }
}
