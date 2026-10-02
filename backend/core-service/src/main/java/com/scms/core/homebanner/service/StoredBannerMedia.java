package com.scms.core.homebanner.service;

public record StoredBannerMedia(
        String mediaPath,
        String mediaContentType,
        long mediaSizeBytes,
        Integer mediaOptimizedVersion,
        String posterPath,
        String posterContentType
) {
}
