package com.scms.core.homebanner.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderHomeBannersRequest(
        @NotEmpty(message = "ids cannot be empty")
        List<Long> ids
) {
}
