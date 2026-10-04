package com.scms.core.iot.repository;

import com.scms.core.iot.domain.IotDeviceTelemetryLatestStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IotDeviceTelemetryLatestStateRepository extends JpaRepository<IotDeviceTelemetryLatestStateEntity, Long> {

    Optional<IotDeviceTelemetryLatestStateEntity> findByDeviceId(String deviceId);

    List<IotDeviceTelemetryLatestStateEntity> findAllByProjectOrderByLastReceivedAtDescDeviceIdAsc(String project);
}
