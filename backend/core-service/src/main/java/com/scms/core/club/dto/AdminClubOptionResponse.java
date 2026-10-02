package com.scms.core.club.dto;

public record AdminClubOptionResponse(
        Long id,
        String name,
        String type,
        String description,
        long memberCount
) {
}
