package com.scms.core.yinzhi.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record YinzhiAiInspectionRunResponse(
        Long inspectionRunId,
        Long telemetryEventId,
        String deviceId,
        Long uploadSequence,
        String triggerReason,
        String ownerPresence,
        String sceneLabel,
        String riskLevel,
        String permissionDecision,
        String actionSummary,
        String aiProvider,
        String aiModel,
        String aiFinishReason,
        int aiPromptTokens,
        int aiCompletionTokens,
        int aiTotalTokens,
        String aiReport,
        boolean fallbackReport,
        String status,
        Instant createdAt,
        Instant updatedAt,
        JsonNode rawTelemetry,
        JsonNode decision
) {
}
