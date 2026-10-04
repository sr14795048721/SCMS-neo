package com.scms.core.homebanner.dto;

import java.util.List;

public record PublicHomeBannersResponse(
        List<PublicHomeBannerItemResponse> banners
) {
}
