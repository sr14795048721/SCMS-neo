package com.scms.core.homebanner.service;

import java.nio.file.Path;

public record BannerMediaContent(
        Path path,
        String contentType,
        long contentLength
) {
}
