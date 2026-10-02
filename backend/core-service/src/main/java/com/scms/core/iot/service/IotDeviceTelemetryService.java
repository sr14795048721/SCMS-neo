package com.scms.core.iot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import com.scms.core.iot.domain.IotDeviceTelemetryLatestStateEntity;
import com.scms.core.iot.dto.CyclingGuardianTelemetryRequest;
import com.scms.core.iot.dto.Esp32TelemetryRequest;
import com.scms.core.iot.dto.IotDeviceTelemetryIngestResponse;
import com.scms.core.iot.dto.IotDeviceTelemetryRequest;
import com.scms.core.iot.repository.IotDeviceTelemetryEventRepository;
import com.scms.core.iot.repository.IotDeviceTelemetryLatestStateRepository;
import com.scms.core.yinzhi.service.YinzhiAiInspectionAsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class IotDeviceTelemetryService {

    public static final String EXPECTED_SCHEMA = "scms.iot.gateway.v1";
    public static final String EXPECTED_PROJECT = "yinzhi_guanjia";
    public static final String ESP32_TELEMETRY_SCHEMA = "gengyu.zhice.telemetry.v1";
    public static final String ESP32_TELEMETRY_PROJECT = "gengyu_zhice";
    public static final String CYCLING_GUARDIAN_TELEMETRY_SCHEMA = "cycling.guardian.telemetry.v1";
    public static final String CYCLING_GUARDIAN_TELEMETRY_PROJECT = "cycling_guardian";
    private static final Duration RECENT_DUPLICATE_WINDOW = Duration.ofMinutes(5);

    private final IotDeviceTelemetryLatestStateRepository latestStateRepository;
    private final IotDeviceTelemetryEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private YinzhiAiInspectionAsyncService yinzhiAiInspectionAsyncService;

    public IotDeviceTelemetryService(IotDeviceTelemetryLatestStateRepository latestStateRepository,
                                     IotDeviceTelemetryEventRepository eventRepository,
                                     ObjectMapper objectMapper) {
        this.latestStateRepository = latestStateRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Autowired
    public void setYinzhiAiInspectionAsyncService(YinzhiAiInspectionAsyncService yinzhiAiInspectionAsyncService) {
        this.yinzhiAiInspectionAsyncService = yinzhiAiInspectionAsyncService;
    }

    @Transactional
    public IotDeviceTelemetryIngestResponse ingest(IotDeviceTelemetryRequest request, String userAgent) {
        validateFixedFields(request);

        String normalizedDeviceId = requireText(request.device().deviceId(), "device telemetry device id is required");
        String normalizedGatewayType = requireText(request.device().gatewayType(), "device telemetry gateway type is required");
        String normalizedEvent = requireText(request.event(), "device telemetry event is required");
        String normalizedSchema = requireText(request.schema(), "device telemetry schema is required");
        String normalizedProject = requireText(request.project(), "device telemetry project is required");
        String normalizedUserAgent = normalizeUserAgent(userAgent);
        String payloadJson = serializePayload(request);
        Long uploadSequence = request.gateway().uploadSequence();

        IotDeviceTelemetryLatestStateEntity latestState = latestStateRepository.findByDeviceId(normalizedDeviceId)
                .orElseGet(() -> {
                    IotDeviceTelemetryLatestStateEntity entity = new IotDeviceTelemetryLatestStateEntity();
                    entity.setDeviceId(normalizedDeviceId);
                    entity.setFirstReceivedAt(Instant.now());
                    return entity;
                });

        if (shouldUpdateLatestState(latestState, request)) {
            applySnapshot(
                    latestState,
                    normalizedSchema,
                    normalizedProject,
                    normalizedDeviceId,
                    normalizedGatewayType,
                    normalizedEvent,
                    normalizedUserAgent,
                    payloadJson,
                    request
            );
            latestStateRepository.save(latestState);
        }

        SavedTelemetryEvent savedEvent = saveEventUnlessRecentDuplicate(
                normalizedDeviceId,
                uploadSequence,
                payloadJson,
                buildEventEntity(
                        normalizedSchema,
                        normalizedProject,
                        normalizedDeviceId,
                        normalizedGatewayType,
                        normalizedEvent,
                        normalizedUserAgent,
                        payloadJson,
                        request
                )
        );

        IotDeviceTelemetryEventEntity telemetryEvent = savedEvent.event();
        Long telemetryEventId = telemetryEvent == null ? null : telemetryEvent.getId();
        if (savedEvent.newlySaved() && yinzhiAiInspectionAsyncService != null) {
            yinzhiAiInspectionAsyncService.inspectAfterCommit(telemetryEventId);
        }
        return new IotDeviceTelemetryIngestResponse(
                true,
                "accepted",
                telemetryEventId,
                null
        );
    }

    @Transactional
    public Long ingest(Esp32TelemetryRequest request, String userAgent) {
        String normalizedGatewayId = requireText(request.gatewayId(), "esp32 telemetry gateway_id is required");
        String normalizedTransport = requireText(request.transport(), "esp32 telemetry transport is required");
        String normalizedMessageType = requireText(request.messageType(), "esp32 telemetry message_type is required");
        String normalizedUserAgent = normalizeUserAgent(userAgent);
        String payloadJson = serializePayload(request);
        Long reportedAtEpochMillis = request.reportedAt().toEpochMilli();

        IotDeviceTelemetryLatestStateEntity latestState = latestStateRepository.findByDeviceId(normalizedGatewayId)
                .orElseGet(() -> {
                    IotDeviceTelemetryLatestStateEntity entity = new IotDeviceTelemetryLatestStateEntity();
                    entity.setDeviceId(normalizedGatewayId);
                    entity.setFirstReceivedAt(Instant.now());
                    return entity;
                });

        if (shouldUpdateLatestState(latestState, reportedAtEpochMillis)) {
            applyEsp32Snapshot(
                    latestState,
                    normalizedGatewayId,
                    normalizedTransport,
                    normalizedMessageType,
                    normalizedUserAgent,
                    payloadJson,
                    reportedAtEpochMillis,
                    request
            );
            latestStateRepository.save(latestState);
        }

        SavedTelemetryEvent savedEvent = saveEventUnlessRecentDuplicate(
                normalizedGatewayId,
                reportedAtEpochMillis,
                payloadJson,
                buildEsp32EventEntity(
                    normalizedGatewayId,
                    normalizedTransport,
                    normalizedMessageType,
                    normalizedUserAgent,
                    payloadJson,
                    reportedAtEpochMillis,
                    request
                )
        );
        IotDeviceTelemetryEventEntity telemetryEvent = savedEvent.event();
        return telemetryEvent == null ? null : telemetryEvent.getId();
    }

    @Transactional
    public void ingest(CyclingGuardianTelemetryRequest request, String userAgent) {
        String normalizedGatewayId = requireText(request.gatewayId(), "cycling guardian telemetry gateway_id is required");
        String normalizedTransport = requireText(request.transport(), "cycling guardian telemetry transport is required");
        String normalizedMessageType = requireText(request.messageType(), "cycling guardian telemetry message_type is required");
        String normalizedUserAgent = normalizeUserAgent(userAgent);
        String payloadJson = serializePayload(request);
        Long reportedAtEpochMillis = request.reportedAt().toEpochMilli();

        IotDeviceTelemetryLatestStateEntity latestState = latestStateRepository.findByDeviceId(normalizedGatewayId)
                .orElseGet(() -> {
                    IotDeviceTelemetryLatestStateEntity entity = new IotDeviceTelemetryLatestStateEntity();
                    entity.setDeviceId(normalizedGatewayId);
                    entity.setFirstReceivedAt(Instant.now());
                    return entity;
                });

        if (shouldUpdateLatestState(latestState, request.uploadSequence(), reportedAtEpochMillis)) {
            applySimpleSnapshot(
                    latestState,
                    CYCLING_GUARDIAN_TELEMETRY_SCHEMA,
                    CYCLING_GUARDIAN_TELEMETRY_PROJECT,
                    normalizedGatewayId,
                    normalizedTransport,
                    normalizedMessageType,
                    normalizedUserAgent,
                    payloadJson,
                    request.uploadSequence(),
                    reportedAtEpochMillis,
                    request.jetsonUptimeMs(),
                    request.sourceDeviceId(),
                    null
            );
            latestStateRepository.save(latestState);
        }

        saveEventUnlessRecentDuplicate(
                normalizedGatewayId,
                request.uploadSequence(),
                payloadJson,
                buildSimpleEventEntity(
                    CYCLING_GUARDIAN_TELEMETRY_SCHEMA,
                    CYCLING_GUARDIAN_TELEMETRY_PROJECT,
                    normalizedGatewayId,
                    normalizedTransport,
                    normalizedMessageType,
                    normalizedUserAgent,
                    payloadJson,
                    request.uploadSequence(),
                    reportedAtEpochMillis,
                    request.jetsonUptimeMs(),
                    request.sourceDeviceId(),
                    null
                )
        );
    }

    private SavedTelemetryEvent saveEventUnlessRecentDuplicate(String deviceId,
                                                               Long uploadSequence,
                                                               String payloadJson,
                                                               IotDeviceTelemetryEventEntity event) {
        Instant duplicateWindowStart = Instant.now().minus(RECENT_DUPLICATE_WINDOW);
        return eventRepository
                .findFirstByDeviceIdAndUploadSequenceAndPayloadJsonAndReceivedAtAfterOrderByReceivedAtDescIdDesc(
                        deviceId,
                        uploadSequence,
                        payloadJson,
                        duplicateWindowStart
                )
                .map(existing -> new SavedTelemetryEvent(existing, false))
                .orElseGet(() -> new SavedTelemetryEvent(eventRepository.save(event), true));
    }

    private record SavedTelemetryEvent(IotDeviceTelemetryEventEntity event, boolean newlySaved) {
    }

    private void validateFixedFields(IotDeviceTelemetryRequest request) {
        String schema = requireText(request.schema(), "device telemetry schema is required");
        if (!EXPECTED_SCHEMA.equals(schema)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "device telemetry schema is invalid");
        }

        String project = requireText(request.project(), "device telemetry project is required");
        if (!EXPECTED_PROJECT.equals(project)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "device telemetry project is invalid");
        }
    }

    private boolean shouldUpdateLatestState(IotDeviceTelemetryLatestStateEntity latestState,
                                            IotDeviceTelemetryRequest request) {
        Long currentUploadSequence = latestState.getUploadSequence();
        Long incomingUploadSequence = request.gateway().uploadSequence();
        if (currentUploadSequence == null || incomingUploadSequence == null) {
            return true;
        }
        if (incomingUploadSequence >= currentUploadSequence) {
            return true;
        }
        return isGatewaySequenceReset(latestState, request);
    }

    private boolean shouldUpdateLatestState(IotDeviceTelemetryLatestStateEntity latestState,
                                            Long incomingUploadSequence) {
        Long currentUploadSequence = latestState.getUploadSequence();
        return currentUploadSequence == null
                || incomingUploadSequence == null
                || incomingUploadSequence >= currentUploadSequence;
    }

    private boolean shouldUpdateLatestState(IotDeviceTelemetryLatestStateEntity latestState,
                                            Long incomingUploadSequence,
                                            Long incomingSentAtMs) {
        Long currentUploadSequence = latestState.getUploadSequence();
        if (currentUploadSequence == null || incomingUploadSequence == null) {
            return true;
        }
        if (incomingUploadSequence >= currentUploadSequence) {
            return true;
        }
        Long currentSentAtMs = latestState.getSentAtMs();
        return currentSentAtMs == null
                || incomingSentAtMs == null
                || incomingSentAtMs > currentSentAtMs;
    }

    private boolean isGatewaySequenceReset(IotDeviceTelemetryLatestStateEntity latestState,
                                           IotDeviceTelemetryRequest request) {
        // ESP32 uploadSequence restarts from 1 after reboot. Let that first
        // successful upload replace the stale latest-state snapshot.
        Long currentUploadSequence = latestState.getUploadSequence();
        Long incomingUploadSequence = request.gateway().uploadSequence();
        if (currentUploadSequence == null || incomingUploadSequence == null) {
            return false;
        }
        if (incomingUploadSequence != 1L || currentUploadSequence <= 1L) {
            return false;
        }

        Long currentUptimeMs = latestState.getGatewayUptimeMs();
        Long incomingUptimeMs = request.gateway().uptimeMs();
        if (currentUptimeMs == null || incomingUptimeMs == null) {
            return true;
        }
        return incomingUptimeMs < currentUptimeMs;
    }

    private void applySnapshot(IotDeviceTelemetryLatestStateEntity entity,
                               String schema,
                               String project,
                               String deviceId,
                               String gatewayType,
                               String event,
                               String userAgent,
                               String payloadJson,
                               IotDeviceTelemetryRequest request) {
        entity.setSchemaName(schema);
        entity.setProject(project);
        entity.setDeviceId(deviceId);
        entity.setGatewayType(gatewayType);
        entity.setFirmware(normalizeOptionalText(request.device().firmware()));
        entity.setMac(normalizeOptionalText(request.device().mac()));
        entity.setEventName(event);
        entity.setUploadSequence(request.gateway().uploadSequence());
        entity.setSentAtMs(request.sentAtMs());
        entity.setGatewayUptimeMs(request.gateway().uptimeMs());
        entity.setPendingReason(normalizeOptionalText(request.gateway().pendingReason()));
        entity.setWifiConnected(Boolean.TRUE.equals(request.network().wifiConnected()));
        entity.setIp(normalizeOptionalText(request.network().ip()));
        entity.setRssi(request.network().rssi());
        entity.setUserAgent(userAgent);
        entity.setPayloadJson(payloadJson);
        entity.setLastReceivedAt(Instant.now());
    }

    private IotDeviceTelemetryEventEntity buildEventEntity(String schema,
                                                           String project,
                                                           String deviceId,
                                                           String gatewayType,
                                                           String event,
                                                           String userAgent,
                                                           String payloadJson,
                                                           IotDeviceTelemetryRequest request) {
        IotDeviceTelemetryEventEntity entity = new IotDeviceTelemetryEventEntity();
        entity.setSchemaName(schema);
        entity.setProject(project);
        entity.setDeviceId(deviceId);
        entity.setGatewayType(gatewayType);
        entity.setFirmware(normalizeOptionalText(request.device().firmware()));
        entity.setMac(normalizeOptionalText(request.device().mac()));
        entity.setEventName(event);
        entity.setUploadSequence(request.gateway().uploadSequence());
        entity.setSentAtMs(request.sentAtMs());
        entity.setGatewayUptimeMs(request.gateway().uptimeMs());
        entity.setPendingReason(normalizeOptionalText(request.gateway().pendingReason()));
        entity.setWifiConnected(Boolean.TRUE.equals(request.network().wifiConnected()));
        entity.setIp(normalizeOptionalText(request.network().ip()));
        entity.setRssi(request.network().rssi());
        entity.setUserAgent(userAgent);
        entity.setPayloadJson(payloadJson);
        return entity;
    }

    private void applyEsp32Snapshot(IotDeviceTelemetryLatestStateEntity entity,
                                    String gatewayId,
                                    String transport,
                                    String messageType,
                                    String userAgent,
                                    String payloadJson,
                                    Long reportedAtEpochMillis,
                                    Esp32TelemetryRequest request) {
        applySimpleSnapshot(
                entity,
                ESP32_TELEMETRY_SCHEMA,
                ESP32_TELEMETRY_PROJECT,
                gatewayId,
                transport,
                messageType,
                userAgent,
                payloadJson,
                reportedAtEpochMillis,
                reportedAtEpochMillis,
                request.esp32UptimeMs(),
                request.sourceDeviceId(),
                request.wifiRssi()
        );
    }

    private IotDeviceTelemetryEventEntity buildEsp32EventEntity(String gatewayId,
                                                                String transport,
                                                                String messageType,
                                                                String userAgent,
                                                                String payloadJson,
                                                                Long reportedAtEpochMillis,
                                                                Esp32TelemetryRequest request) {
        return buildSimpleEventEntity(
                ESP32_TELEMETRY_SCHEMA,
                ESP32_TELEMETRY_PROJECT,
                gatewayId,
                transport,
                messageType,
                userAgent,
                payloadJson,
                reportedAtEpochMillis,
                reportedAtEpochMillis,
                request.esp32UptimeMs(),
                request.sourceDeviceId(),
                request.wifiRssi()
        );
    }

    private void applySimpleSnapshot(IotDeviceTelemetryLatestStateEntity entity,
                                     String schema,
                                     String project,
                                     String deviceId,
                                     String gatewayType,
                                     String event,
                                     String userAgent,
                                     String payloadJson,
                                     Long uploadSequence,
                                     Long sentAtMs,
                                     Long gatewayUptimeMs,
                                     String pendingReason,
                                     Integer rssi) {
        entity.setSchemaName(schema);
        entity.setProject(project);
        entity.setDeviceId(deviceId);
        entity.setGatewayType(gatewayType);
        entity.setFirmware(null);
        entity.setMac(null);
        entity.setEventName(event);
        entity.setUploadSequence(uploadSequence);
        entity.setSentAtMs(sentAtMs);
        entity.setGatewayUptimeMs(gatewayUptimeMs);
        entity.setPendingReason(normalizeOptionalText(pendingReason));
        entity.setWifiConnected(true);
        entity.setIp(null);
        entity.setRssi(rssi);
        entity.setUserAgent(userAgent);
        entity.setPayloadJson(payloadJson);
        entity.setLastReceivedAt(Instant.now());
    }

    private IotDeviceTelemetryEventEntity buildSimpleEventEntity(String schema,
                                                                 String project,
                                                                 String deviceId,
                                                                 String gatewayType,
                                                                 String event,
                                                                 String userAgent,
                                                                 String payloadJson,
                                                                 Long uploadSequence,
                                                                 Long sentAtMs,
                                                                 Long gatewayUptimeMs,
                                                                 String pendingReason,
                                                                 Integer rssi) {
        IotDeviceTelemetryEventEntity entity = new IotDeviceTelemetryEventEntity();
        entity.setSchemaName(schema);
        entity.setProject(project);
        entity.setDeviceId(deviceId);
        entity.setGatewayType(gatewayType);
        entity.setFirmware(null);
        entity.setMac(null);
        entity.setEventName(event);
        entity.setUploadSequence(uploadSequence);
        entity.setSentAtMs(sentAtMs);
        entity.setGatewayUptimeMs(gatewayUptimeMs);
        entity.setPendingReason(normalizeOptionalText(pendingReason));
        entity.setWifiConnected(true);
        entity.setIp(null);
        entity.setRssi(rssi);
        entity.setUserAgent(userAgent);
        entity.setPayloadJson(payloadJson);
        return entity;
    }

    private String serializePayload(IotDeviceTelemetryRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize iot device telemetry payload failed", exception);
        }
    }

    private String serializePayload(Esp32TelemetryRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize esp32 telemetry payload failed", exception);
        }
    }

    private String serializePayload(CyclingGuardianTelemetryRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize cycling guardian telemetry payload failed", exception);
        }
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUserAgent(String userAgent) {
        String normalized = normalizeText(userAgent);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
