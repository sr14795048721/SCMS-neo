package com.scms.core.homeclubrecommend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateHomeClubRecommendationsRequest(
        @Size(max = 4, message = "recommended clubs can contain at most 4 items")
        List<@NotNull(message = "club id is required") Long> clubIds
) {
}
