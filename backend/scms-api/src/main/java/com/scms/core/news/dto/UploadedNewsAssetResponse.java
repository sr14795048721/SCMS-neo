package com.scms.core.news.dto;

public record UploadedNewsAssetResponse(
        Long assetId,
        String url,
        String markdown
) {
}
