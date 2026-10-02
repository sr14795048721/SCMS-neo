package com.scms.core.homebanner.dto;

import java.util.List;

public record AdminBannerRecompressResponse(
        int processedCount,
        int skippedCount,
        int failedCount,
        List<AdminBannerRecompressFailureResponse> failures
) {
}
