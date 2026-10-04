package com.scms.core.yinzhi.repository;

import com.scms.core.yinzhi.domain.YinzhiAiInspectionRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface YinzhiAiInspectionRunRepository extends JpaRepository<YinzhiAiInspectionRunEntity, Long> {

    boolean existsByTelemetryEventId(Long telemetryEventId);

    boolean existsByDeviceIdAndTriggerReasonAndSceneLabelAndRiskLevelAndCreatedAtAfter(
            String deviceId,
            String triggerReason,
            String sceneLabel,
            String riskLevel,
            Instant createdAt
    );

    Optional<YinzhiAiInspectionRunEntity> findFirstByOrderByCreatedAtDescIdDesc();

    List<YinzhiAiInspectionRunEntity> findTop10ByOrderByCreatedAtDescIdDesc();
}
