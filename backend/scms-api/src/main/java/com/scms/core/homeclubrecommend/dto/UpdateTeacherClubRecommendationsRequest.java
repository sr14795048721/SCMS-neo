package com.scms.core.homeclubrecommend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTeacherClubRecommendationsRequest(
        @NotNull
        @Size(max = 4)
        List<@NotNull Long> clubIds
) {
}
