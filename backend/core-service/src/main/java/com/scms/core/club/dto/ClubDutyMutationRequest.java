package com.scms.core.club.dto;

import java.util.List;

public record ClubDutyMutationRequest(
        String name,
        List<String> permissions
) {
}
