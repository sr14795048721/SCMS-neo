package com.scms.core.yinzhi.dto;

import java.time.Instant;

public record YinzhiAiInspectionRunSummaryResponse(
        Long inspectionRunId,
        Long telemetryEventId,
        String deviceId,
        Long uploadSequence,
        String sceneLabel,
        String riskLevel,
        String status,
        Instant createdAt
) {
}
