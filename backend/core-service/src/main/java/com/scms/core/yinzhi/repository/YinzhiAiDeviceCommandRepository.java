package com.scms.core.yinzhi.repository;

import com.scms.core.yinzhi.domain.YinzhiAiDeviceCommandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface YinzhiAiDeviceCommandRepository extends JpaRepository<YinzhiAiDeviceCommandEntity, Long> {

    List<YinzhiAiDeviceCommandEntity> findAllByRunIdOrderByQueuedAtAscIdAsc(Long runId);

    List<YinzhiAiDeviceCommandEntity> findAllByVerificationTelemetryEventIdOrderByQueuedAtAscIdAsc(Long verificationTelemetryEventId);

    List<YinzhiAiDeviceCommandEntity> findTop20ByDeviceIdAndStatusInOrderByQueuedAtDescIdDesc(
            String deviceId,
            List<String> statuses
    );

    Optional<YinzhiAiDeviceCommandEntity> findFirstByDeviceIdAndStatusOrderByQueuedAtAscIdAsc(String deviceId, String status);

    List<YinzhiAiDeviceCommandEntity> findAllByDeviceIdAndStatusInAndUploadSequenceLessThanOrderByQueuedAtAscIdAsc(
            String deviceId,
            List<String> statuses,
            Long uploadSequence
    );
}
