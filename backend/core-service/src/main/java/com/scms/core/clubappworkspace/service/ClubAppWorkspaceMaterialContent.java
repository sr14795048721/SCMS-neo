package com.scms.core.clubappworkspace.service;

import java.io.InputStream;

public record ClubAppWorkspaceMaterialContent(
        InputStream inputStream,
        String contentType,
        long contentLength,
        String fileName
) {
}
