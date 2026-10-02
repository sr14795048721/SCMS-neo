package com.scms.core.yinzhi.dto;

public record YinzhiDeviceCommandPollResponse(
        boolean hasCommand,
        Long commandId,
        Long inspectionRunId,
        Long telemetryEventId,
        Long uploadSequence,
        String commandType,
        String riskLevel,
        String detail
) {
    public static YinzhiDeviceCommandPollResponse empty() {
        return new YinzhiDeviceCommandPollResponse(false, null, null, null, null, "", "", "");
    }
}
