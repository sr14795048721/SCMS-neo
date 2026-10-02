package com.scms.core.news.service;

import java.nio.file.Path;

public record NewsMediaContent(
        Path path,
        String contentType,
        long contentLength
) {
}
