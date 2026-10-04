package com.scms.core.homebanner.dto;

public record PublicHomeBannerItemResponse(
        Long id,
        String type,
        String title,
        String desc,
        String src,
        String poster
) {
}
