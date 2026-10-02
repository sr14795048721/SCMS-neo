package com.scms.core.iot.repository;

import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IotDeviceTelemetryEventRepository extends JpaRepository<IotDeviceTelemetryEventEntity, Long> {

    Optional<IotDeviceTelemetryEventEntity> findFirstByDeviceIdAndUploadSequenceAndPayloadJsonAndReceivedAtAfterOrderByReceivedAtDescIdDesc(
            String deviceId,
            Long uploadSequence,
            String payloadJson,
            Instant receivedAfter
    );

    Page<IotDeviceTelemetryEventEntity> findAllByProjectOrderByReceivedAtDescIdDesc(String project, Pageable pageable);

    List<IotDeviceTelemetryEventEntity> findAllByProjectAndReceivedAtAfterOrderByReceivedAtAscIdAsc(
            String project,
            Instant receivedAfter
    );
}
