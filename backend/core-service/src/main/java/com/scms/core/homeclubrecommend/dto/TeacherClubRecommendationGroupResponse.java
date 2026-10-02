package com.scms.core.homeclubrecommend.dto;

import java.util.List;

public record TeacherClubRecommendationGroupResponse(
        Long managerUserId,
        String managerName,
        String managerNo,
        List<TeacherClubRecommendationItemResponse> recommendations
) {
}
