package com.scms.core.homeclubrecommend.dto;

public record AdminHomeClubRecommendationResponse(
        short slotNo,
        Long clubId,
        String name,
        String type,
        String description,
        long memberCount
) {
}
