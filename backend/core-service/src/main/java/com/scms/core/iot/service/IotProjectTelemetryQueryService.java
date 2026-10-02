package com.scms.core.iot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceGroupEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectEntity;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceGroupRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import com.scms.core.iot.domain.IotDeviceTelemetryLatestStateEntity;
import com.scms.core.iot.dto.IotProjectTelemetryDeviceResponse;
import com.scms.core.iot.dto.IotProjectTelemetryEventResponse;
import com.scms.core.iot.dto.IotProjectTelemetryResponse;
import com.scms.core.iot.repository.IotDeviceTelemetryEventRepository;
import com.scms.core.iot.repository.IotDeviceTelemetryLatestStateRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class IotProjectTelemetryQueryService {

    private static final String INTEGRATION_MODE_GATEWAY = "ESP32_GATEWAY";
    private static final String INTEGRATION_MODE_NONE = "NONE";
    private static final int DEFAULT_EVENT_PAGE = 0;
    private static final int DEFAULT_EVENT_SIZE = 24;
    private static final int MAX_EVENT_SIZE = 60;
    private static final Set<String> GATEWAY_TELEMETRY_PROJECTS = Set.of(
            IotDeviceTelemetryService.EXPECTED_PROJECT,
            IotDeviceTelemetryService.ESP32_TELEMETRY_PROJECT,
            IotDeviceTelemetryService.CYCLING_GUARDIAN_TELEMETRY_PROJECT
    );

    private final IotDeviceTelemetryLatestStateRepository latestStateRepository;
    private final IotDeviceTelemetryEventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubAppWorkspaceGroupRepository groupRepository;
    private final ClubAppWorkspaceProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public IotProjectTelemetryQueryService(
            IotDeviceTelemetryLatestStateRepository latestStateRepository,
            IotDeviceTelemetryEventRepository eventRepository,
            ClubRepository clubRepository,
            ClubManagerBindingRepository clubManagerBindingRepository,
            ClubStudentMemberRepository clubStudentMemberRepository,
            ClubAppWorkspaceGroupRepository groupRepository,
            ClubAppWorkspaceProjectRepository projectRepository,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.latestStateRepository = latestStateRepository;
        this.eventRepository = eventRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.groupRepository = groupRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public IotProjectTelemetryResponse getVisibleProjectTelemetry(Long clubId, String projectKey) {
        return getVisibleProjectTelemetry(clubId, projectKey, DEFAULT_EVENT_PAGE, DEFAULT_EVENT_SIZE);
    }

    @Transactional(readOnly = true)
    public IotProjectTelemetryResponse getVisibleProjectTelemetry(Long clubId, String projectKey, Integer eventPage, Integer eventSize) {
        requireViewerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredVisibleProject(clubId, projectKey);

        int normalizedEventPage = normalizeEventPage(eventPage);
        int normalizedEventSize = normalizeEventSize(eventSize);
        String normalizedProjectKey = normalizeProjectKey(project.getProjectKey());
        String telemetryProjectKey = toTelemetryProjectKey(normalizedProjectKey);
        List<IotProjectTelemetryDeviceResponse> devices =
                latestStateRepository.findAllByProjectOrderByLastReceivedAtDescDeviceIdAsc(telemetryProjectKey).stream()
                        .map(this::toDeviceResponse)
                        .toList();
        Page<IotDeviceTelemetryEventEntity> eventPageResult =
                eventRepository.findAllByProjectOrderByReceivedAtDescIdDesc(
                        telemetryProjectKey,
                        PageRequest.of(normalizedEventPage, normalizedEventSize)
                );
        List<IotProjectTelemetryEventResponse> recentEvents = eventPageResult.stream()
                        .map(this::toEventResponse)
                        .toList();

        return new IotProjectTelemetryResponse(
                normalizedProjectKey,
                telemetryProjectKey,
                resolveIntegrationMode(telemetryProjectKey),
                !devices.isEmpty() || !recentEvents.isEmpty(),
                devices,
                recentEvents,
                normalizedEventPage,
                normalizedEventSize,
                eventPageResult.getTotalElements(),
                eventPageResult.hasNext()
        );
    }

    private IotProjectTelemetryDeviceResponse toDeviceResponse(IotDeviceTelemetryLatestStateEntity entity) {
        JsonNode payload = parsePayload(entity.getPayloadJson());
        return new IotProjectTelemetryDeviceResponse(
                entity.getDeviceId(),
                entity.getGatewayType(),
                entity.getFirmware(),
                entity.getMac(),
                entity.getEventName(),
                entity.getUploadSequence(),
                entity.getSentAtMs(),
                entity.getGatewayUptimeMs(),
                entity.getPendingReason(),
                Boolean.TRUE.equals(entity.getWifiConnected()),
                entity.getIp(),
                entity.getRssi(),
                entity.getLastReceivedAt(),
                extractEdgeInference(payload),
                payload
        );
    }

    private IotProjectTelemetryEventResponse toEventResponse(IotDeviceTelemetryEventEntity entity) {
        JsonNode payload = parsePayload(entity.getPayloadJson());
        return new IotProjectTelemetryEventResponse(
                entity.getId(),
                entity.getDeviceId(),
                entity.getEventName(),
                entity.getUploadSequence(),
                entity.getSentAtMs(),
                entity.getReceivedAt(),
                entity.getPendingReason(),
                Boolean.TRUE.equals(entity.getWifiConnected()),
                entity.getIp(),
                entity.getRssi(),
                extractEdgeInference(payload),
                payload
        );
    }

    private JsonNode parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return JsonNodeFactory.instance.objectNode();
        }
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException exception) {
            return JsonNodeFactory.instance.objectNode();
        }
    }

    private JsonNode extractEdgeInference(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return JsonNodeFactory.instance.objectNode();
        }
        JsonNode edgeInference = payload.path("edge_inference");
        return edgeInference.isObject() ? edgeInference : JsonNodeFactory.instance.objectNode();
    }

    private String resolveIntegrationMode(String telemetryProjectKey) {
        return GATEWAY_TELEMETRY_PROJECTS.contains(telemetryProjectKey)
                ? INTEGRATION_MODE_GATEWAY
                : INTEGRATION_MODE_NONE;
    }

    private String toTelemetryProjectKey(String projectKey) {
        return normalizeProjectKey(projectKey).replace('-', '_');
    }

    private ClubAppWorkspaceProjectEntity getRequiredVisibleProject(Long clubId, String projectKey) {
        List<Long> groupIds = groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(clubId).stream()
                .map(ClubAppWorkspaceGroupEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (groupIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "workspace project not found");
        }
        return projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(groupIds, normalizeProjectKey(projectKey))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "workspace project not found"));
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private AuthenticatedUser requireViewerAccessToClub(Long clubId) {
        getRequiredClub(clubId);
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() == UserRole.STUDENT) {
            if (!clubStudentMemberRepository.existsByClubIdAndStudentUserId(clubId, user.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "club workspace permission denied");
            }
            return user;
        }
        if (user.role() == UserRole.CLUB_MANAGER) {
            if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, user.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "club workspace permission denied");
            }
            return user;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "club workspace permission denied");
    }

    private String normalizeProjectKey(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project key is invalid");
        }
        return normalized;
    }

    private int normalizeEventPage(Integer value) {
        return value == null || value < 0 ? DEFAULT_EVENT_PAGE : value;
    }

    private int normalizeEventSize(Integer value) {
        if (value == null || value <= 0) {
            return DEFAULT_EVENT_SIZE;
        }
        return Math.min(value, MAX_EVENT_SIZE);
    }
}
