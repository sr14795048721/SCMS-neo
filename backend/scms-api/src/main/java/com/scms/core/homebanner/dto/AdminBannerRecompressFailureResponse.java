package com.scms.core.homebanner.dto;

public record AdminBannerRecompressFailureResponse(
        Long bannerId,
        String reason
) {
}
