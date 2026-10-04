package com.scms.core.yinzhi.dto;

import java.time.Instant;

public record YinzhiAiDeviceCommandResponse(
        Long commandId,
        String commandType,
        String riskLevel,
        String status,
        String detail,
        Instant queuedAt,
        Instant deliveredAt,
        Instant ackedAt,
        String ackMessage,
        Instant verifiedAt,
        Long verificationTelemetryEventId,
        Long verificationUploadSequence,
        String verificationMessage
) {
}
