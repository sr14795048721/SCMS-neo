package com.scms.core.homebanner.dto;

public record AdminHomeBannerResponse(
        Long id,
        String type,
        String title,
        String desc,
        String src,
        String poster,
        Integer sortOrder,
        boolean hasMedia
) {
}
