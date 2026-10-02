package com.scms.core.yinzhi.dto;

import java.time.Instant;
import java.util.List;

public record YinzhiAiInspectionResponse(
        Instant serverTime,
        boolean hasInspection,
        YinzhiAiInspectionRunResponse latestRun,
        List<YinzhiAiInspectionRunSummaryResponse> recentRuns,
        List<YinzhiAiInspectionLogResponse> logs,
        List<YinzhiAiDeviceCommandResponse> commands
) {
}
