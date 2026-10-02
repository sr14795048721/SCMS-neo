package com.scms.core.yinzhi.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record YinzhiAiInspectionLogResponse(
        Long id,
        String stage,
        String level,
        String message,
        Instant createdAt,
        JsonNode raw
) {
}
