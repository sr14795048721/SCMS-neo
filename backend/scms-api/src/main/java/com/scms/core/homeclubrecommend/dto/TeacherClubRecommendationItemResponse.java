package com.scms.core.homeclubrecommend.dto;

public record TeacherClubRecommendationItemResponse(
        int orderNo,
        Long clubId,
        String clubName,
        String clubType,
        String description,
        long memberCount,
        String remark
) {
}
