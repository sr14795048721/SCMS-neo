package com.scms.core.news.service;

public record StoredNewsFile(
        String relativePath,
        String contentType,
        long sizeBytes
) {
}
