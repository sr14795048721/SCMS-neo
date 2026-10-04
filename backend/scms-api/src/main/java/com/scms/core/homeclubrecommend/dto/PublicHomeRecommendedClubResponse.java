package com.scms.core.homeclubrecommend.dto;

public record PublicHomeRecommendedClubResponse(
        Long id,
        String name,
        String type,
        String description,
        long memberCount
) {
}
