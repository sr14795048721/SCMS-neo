package com.scms.core.clubappworkspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceGroupEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoEventEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoProfileEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoStepEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectEntity;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRiskSyncRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeEventRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeEventResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoStepResponse;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoRequest;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoResponse;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoStepRequest;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoStepResponse;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceGroupRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoEventRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoProfileRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoRuntimeSnapshotRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoStepRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ClubAppWorkspaceProjectDemoService {

    private static final int MAX_EVENT_HISTORY = 40;
    private static final String DEFAULT_BRIDGE_MODE = "APP_HUB";

    private final ClubAppWorkspaceGroupRepository groupRepository;
    private final ClubAppWorkspaceProjectRepository projectRepository;
    private final ClubAppWorkspaceProjectDemoProfileRepository profileRepository;
    private final ClubAppWorkspaceProjectDemoStepRepository stepRepository;
    private final ClubAppWorkspaceProjectDemoRuntimeSnapshotRepository runtimeSnapshotRepository;
    private final ClubAppWorkspaceProjectDemoEventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public ClubAppWorkspaceProjectDemoService(
            ClubAppWorkspaceGroupRepository groupRepository,
            ClubAppWorkspaceProjectRepository projectRepository,
            ClubAppWorkspaceProjectDemoProfileRepository profileRepository,
            ClubAppWorkspaceProjectDemoStepRepository stepRepository,
            ClubAppWorkspaceProjectDemoRuntimeSnapshotRepository runtimeSnapshotRepository,
            ClubAppWorkspaceProjectDemoEventRepository eventRepository,
            ClubRepository clubRepository,
            ClubManagerBindingRepository clubManagerBindingRepository,
            ClubStudentMemberRepository clubStudentMemberRepository,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.groupRepository = groupRepository;
        this.projectRepository = projectRepository;
        this.profileRepository = profileRepository;
        this.stepRepository = stepRepository;
        this.runtimeSnapshotRepository = runtimeSnapshotRepository;
        this.eventRepository = eventRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ClubAppWorkspaceProjectDemoResponse getVisibleProjectDemo(Long clubId, String projectKey) {
        requireViewerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredVisibleProject(clubId, projectKey);
        ClubAppWorkspaceProjectDemoProfileEntity profile =
                profileRepository.findFirstByProjectIdAndEnabledTrue(project.getId()).orElse(null);

        return new ClubAppWorkspaceProjectDemoResponse(
                project.getId(),
                project.getProjectKey(),
                project.getTitle(),
                project.getSubtitle(),
                project.getCoverUrl(),
                profile == null ? fallbackOverviewTitle(project) : profile.getOverviewTitle(),
                profile == null ? fallbackOverviewBody(project) : profile.getOverviewBody(),
                profile == null ? DEFAULT_BRIDGE_MODE : profile.getBridgeMode(),
                profile == null
                        ? List.of()
                        : stepRepository.findAllByDemoProfileIdAndEnabledTrueOrderBySortOrderAscIdAsc(profile.getId()).stream()
                        .map(this::toVisibleStepResponse)
                        .toList(),
                profile == null ? buildFallbackMockSnapshot(project) : parseJson(profile.getMockSnapshotJson(), buildFallbackMockSnapshot(project))
        );
    }

    @Transactional(readOnly = true)
    public ManagerClubAppWorkspaceProjectDemoResponse getManagerProjectDemo(Long clubId, String projectKey) {
        requireManagerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredProject(clubId, projectKey);
        ClubAppWorkspaceProjectDemoProfileEntity profile = profileRepository.findFirstByProjectId(project.getId()).orElse(null);

        return toManagerDemoResponse(project, profile);
    }

    @Transactional
    public ManagerClubAppWorkspaceProjectDemoResponse saveManagerProjectDemo(
            Long clubId,
            String projectKey,
            ManagerClubAppWorkspaceProjectDemoRequest request
    ) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredProject(clubId, projectKey);
        ClubAppWorkspaceProjectDemoProfileEntity profile = profileRepository.findFirstByProjectId(project.getId())
                .orElseGet(ClubAppWorkspaceProjectDemoProfileEntity::new);

        NormalizedDemoInput input = normalizeDemoRequest(request, project, profile);

        boolean creatingProfile = profile.getId() == null;
        if (creatingProfile) {
            profile.setProjectId(project.getId());
            profile.setCreatedBy(manager.userId());
        }
        profile.setOverviewTitle(input.overviewTitle());
        profile.setOverviewBody(input.overviewBody());
        profile.setBridgeMode(input.bridgeMode());
        profile.setMockSnapshotJson(writeJson(input.mockSnapshot()));
        profile.setEnabled(input.enabled());
        profile.setUpdatedBy(manager.userId());
        ClubAppWorkspaceProjectDemoProfileEntity savedProfile = profileRepository.save(profile);

        List<ClubAppWorkspaceProjectDemoStepEntity> existingSteps =
                stepRepository.findAllByDemoProfileIdOrderBySortOrderAscIdAsc(savedProfile.getId());
        Map<Long, ClubAppWorkspaceProjectDemoStepEntity> existingStepMap = toStepMap(existingSteps);
        LinkedHashSet<Long> keptStepIds = new LinkedHashSet<>();
        List<ClubAppWorkspaceProjectDemoStepEntity> stepsToSave = new ArrayList<>();
        for (int index = 0; index < input.steps().size(); index++) {
            NormalizedStepInput stepInput = input.steps().get(index);
            ClubAppWorkspaceProjectDemoStepEntity entity = stepInput.id() == null
                    ? new ClubAppWorkspaceProjectDemoStepEntity()
                    : requireExistingStep(existingStepMap, stepInput.id());
            if (stepInput.id() != null) {
                keptStepIds.add(stepInput.id());
            }
            entity.setDemoProfileId(savedProfile.getId());
            entity.setTitle(stepInput.title());
            entity.setDescription(stepInput.description());
            entity.setTriggerType(stepInput.triggerType());
            entity.setTargetSubsystem(stepInput.targetSubsystem());
            entity.setActionKey(stepInput.actionKey());
            entity.setEnabled(stepInput.enabled());
            entity.setSortOrder(index + 1);
            if (stepInput.id() == null) {
                entity.setCreatedBy(manager.userId());
            }
            entity.setUpdatedBy(manager.userId());
            stepsToSave.add(entity);
        }
        deleteStaleSteps(existingSteps, keptStepIds);
        if (!stepsToSave.isEmpty()) {
            stepRepository.saveAll(stepsToSave);
        }

        ClubAppWorkspaceProjectDemoProfileEntity refreshedProfile =
                profileRepository.findFirstByProjectId(project.getId()).orElse(savedProfile);
        return toManagerDemoResponse(project, refreshedProfile);
    }

    @Transactional(readOnly = true)
    public ClubAppWorkspaceProjectDemoRuntimeResponse getVisibleProjectDemoRuntime(Long clubId, String projectKey) {
        requireViewerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredVisibleProject(clubId, projectKey);
        return buildRuntimeResponse(project);
    }

    @Transactional
    public ClubAppWorkspaceProjectDemoRuntimeResponse saveVisibleProjectDemoRuntime(
            Long clubId,
            String projectKey,
            ClubAppWorkspaceProjectDemoRuntimeRequest request
    ) {
        AuthenticatedUser user = requireViewerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredVisibleProject(clubId, projectKey);
        NormalizedRuntimeInput input = normalizeRuntimeRequest(request, project);

        ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity snapshot =
                runtimeSnapshotRepository.findFirstByProjectId(project.getId())
                        .orElseGet(ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity::new);
        if (snapshot.getId() == null) {
            snapshot.setProjectId(project.getId());
            snapshot.setCreatedBy(user.userId());
        }
        snapshot.setSourceType(input.sourceType());
        snapshot.setSourceSessionId(input.sessionId());
        snapshot.setPayloadJson(writeJson(input.payload()));
        runtimeSnapshotRepository.save(snapshot);

        if (!input.events().isEmpty()) {
            List<ClubAppWorkspaceProjectDemoEventEntity> eventsToSave = input.events().stream()
                    .map(event -> toEventEntity(project.getId(), input.sessionId(), event))
                    .toList();
            eventRepository.saveAll(eventsToSave);
            trimEventHistory(project.getId());
        }

        return buildRuntimeResponse(project);
    }

    @Transactional
    public ClubAppWorkspaceProjectDemoRuntimeResponse saveBridgeProjectDemoRiskRuntime(
            ClubAppWorkspaceProjectDemoRiskSyncRequest request
    ) {
        Long clubId = request == null ? null : request.clubId();
        if (clubId == null || clubId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "project demo risk sync club id is required");
        }

        ClubAppWorkspaceProjectEntity project = getRequiredProject(clubId, request.projectKey());
        NormalizedRiskSyncInput input = normalizeRiskSyncRequest(request, project);
        JsonNode fallback = profileRepository.findFirstByProjectId(project.getId())
                .map(profile -> parseJson(profile.getMockSnapshotJson(), buildFallbackMockSnapshot(project)))
                .orElseGet(() -> buildFallbackMockSnapshot(project));

        ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity snapshot =
                runtimeSnapshotRepository.findFirstByProjectId(project.getId())
                        .orElseGet(ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity::new);
        JsonNode currentPayload = snapshot.getId() == null
                ? fallback
                : parseJson(snapshot.getPayloadJson(), fallback);

        if (snapshot.getId() == null) {
            snapshot.setProjectId(project.getId());
            Long operatorUserId = project.getUpdatedBy() != null ? project.getUpdatedBy() : project.getCreatedBy();
            if (operatorUserId == null || operatorUserId <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "project demo operator user is missing");
            }
            snapshot.setCreatedBy(operatorUserId);
        }
        snapshot.setSourceType(input.sourceType());
        snapshot.setSourceSessionId(input.sessionId());
        snapshot.setPayloadJson(writeJson(mergeRiskRuntimePayload(currentPayload, input)));
        runtimeSnapshotRepository.save(snapshot);

        if (!input.events().isEmpty()) {
            List<ClubAppWorkspaceProjectDemoEventEntity> eventsToSave = input.events().stream()
                    .map(event -> toEventEntity(project.getId(), input.sessionId(), event))
                    .toList();
            eventRepository.saveAll(eventsToSave);
            trimEventHistory(project.getId());
        }

        return buildRuntimeResponse(project);
    }

    @Transactional
    public void ensureDefaultCyclingGuardianDemo() {
        Optional<ClubAppWorkspaceProjectEntity> optionalProject =
                projectRepository.findFirstByProjectKey("cycling-guardian");
        if (optionalProject.isEmpty()) {
            return;
        }

        ClubAppWorkspaceProjectEntity project = optionalProject.get();
        if (profileRepository.findFirstByProjectId(project.getId()).isPresent()) {
            return;
        }

        Long operatorUserId = project.getUpdatedBy() != null ? project.getUpdatedBy() : project.getCreatedBy();
        if (operatorUserId == null || operatorUserId <= 0) {
            return;
        }

        ClubAppWorkspaceProjectDemoProfileEntity profile = new ClubAppWorkspaceProjectDemoProfileEntity();
        profile.setProjectId(project.getId());
        profile.setOverviewTitle("骑行守护者 Demo");
        profile.setOverviewBody("""
                骑行守护者 Demo 以修远科技社的比赛方案为基础，围绕道路风险识别、腕表健康监测、智能联动提醒和云端分析建议四段演示展开。
                App 作为演示中枢，负责聚合腕表侧 BLE 数据与头盔侧局域网快照，并把最新状态同步到 SCMS，便于现场展示与教师端维护。
                """.trim());
        profile.setBridgeMode(DEFAULT_BRIDGE_MODE);
        profile.setMockSnapshotJson(writeJson(buildCyclingGuardianMockSnapshot()));
        profile.setEnabled(true);
        profile.setCreatedBy(operatorUserId);
        profile.setUpdatedBy(operatorUserId);
        ClubAppWorkspaceProjectDemoProfileEntity savedProfile = profileRepository.save(profile);

        List<ClubAppWorkspaceProjectDemoStepEntity> steps = List.of(
                buildStep(savedProfile.getId(), "道路识别", "展示头盔视觉模组对车辆、行人和近距风险的识别结果。", "SCENE", "HELMET", null, 1, operatorUserId),
                buildStep(savedProfile.getId(), "腕表健康", "展示腕表侧心率、血氧与提醒状态，血压在 v1 中不作为真实采集指标。", "AUTO", "WATCH", null, 2, operatorUserId),
                buildStep(savedProfile.getId(), "风险联动", "展示灯带、App 导航提示与联动告警的同步变化。", "MANUAL", "APP", null, 3, operatorUserId),
                buildStep(savedProfile.getId(), "云端建议", "展示 SCMS 对当前骑行状态的规则化分析建议。", "MANUAL", "CLOUD", null, 4, operatorUserId)
        );
        stepRepository.saveAll(steps);
    }

    private ManagerClubAppWorkspaceProjectDemoResponse toManagerDemoResponse(
            ClubAppWorkspaceProjectEntity project,
            ClubAppWorkspaceProjectDemoProfileEntity profile
    ) {
        if (profile == null) {
            return new ManagerClubAppWorkspaceProjectDemoResponse(
                    project.getId(),
                    project.getProjectKey(),
                    project.getTitle(),
                    project.getSubtitle(),
                    project.getCoverUrl(),
                    fallbackOverviewTitle(project),
                    fallbackOverviewBody(project),
                    DEFAULT_BRIDGE_MODE,
                    false,
                    List.of(),
                    buildFallbackMockSnapshot(project)
            );
        }

        return new ManagerClubAppWorkspaceProjectDemoResponse(
                project.getId(),
                project.getProjectKey(),
                project.getTitle(),
                project.getSubtitle(),
                project.getCoverUrl(),
                profile.getOverviewTitle(),
                profile.getOverviewBody(),
                profile.getBridgeMode(),
                Boolean.TRUE.equals(profile.getEnabled()),
                stepRepository.findAllByDemoProfileIdOrderBySortOrderAscIdAsc(profile.getId()).stream()
                        .map(this::toManagerStepResponse)
                        .toList(),
                parseJson(profile.getMockSnapshotJson(), buildFallbackMockSnapshot(project))
        );
    }

    private ClubAppWorkspaceProjectDemoRuntimeResponse buildRuntimeResponse(ClubAppWorkspaceProjectEntity project) {
        ClubAppWorkspaceProjectDemoProfileEntity profile = profileRepository.findFirstByProjectId(project.getId()).orElse(null);
        JsonNode fallback = profile == null
                ? buildFallbackMockSnapshot(project)
                : parseJson(profile.getMockSnapshotJson(), buildFallbackMockSnapshot(project));
        ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity snapshot =
                runtimeSnapshotRepository.findFirstByProjectId(project.getId()).orElse(null);
        JsonNode runtimePayload = snapshot == null ? fallback : parseJson(snapshot.getPayloadJson(), fallback);
        List<ClubAppWorkspaceProjectDemoRuntimeEventResponse> events =
                eventRepository.findTop12ByProjectIdOrderByEventAtDescIdDesc(project.getId()).stream()
                        .map(event -> new ClubAppWorkspaceProjectDemoRuntimeEventResponse(
                                event.getId(),
                                event.getEventType(),
                                event.getTitle(),
                                event.getDetail(),
                                event.getLevel(),
                                event.getEventAt()
                        ))
                        .toList();

        return new ClubAppWorkspaceProjectDemoRuntimeResponse(
                project.getProjectKey(),
                readText(runtimePayload.get("sessionId"), snapshot == null ? "" : snapshot.getSourceSessionId()),
                readObject(runtimePayload, "watch"),
                readObject(runtimePayload, "helmet"),
                readObject(runtimePayload, "app"),
                readArray(runtimePayload, "alerts"),
                readObject(runtimePayload, "recommendation"),
                events,
                snapshot == null ? Instant.now() : snapshot.getUpdatedAt()
        );
    }

    private NormalizedDemoInput normalizeDemoRequest(
            ManagerClubAppWorkspaceProjectDemoRequest request,
            ClubAppWorkspaceProjectEntity project,
            ClubAppWorkspaceProjectDemoProfileEntity existingProfile
    ) {
        List<ManagerClubAppWorkspaceProjectDemoStepRequest> rawSteps =
                request == null || request.steps() == null ? List.of() : request.steps();

        LinkedHashSet<Long> seenStepIds = new LinkedHashSet<>();
        List<NormalizedStepInput> steps = new ArrayList<>();
        for (ManagerClubAppWorkspaceProjectDemoStepRequest item : rawSteps) {
            Long id = normalizeId(item == null ? null : item.id());
            if (id != null && !seenStepIds.add(id)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "duplicate project demo step ids");
            }
            steps.add(new NormalizedStepInput(
                    id,
                    requireText(item == null ? null : item.title(), "project demo step title is required"),
                    normalizeText(item == null ? null : item.description()),
                    normalizeMode(item == null ? null : item.triggerType(), "project demo step trigger type is required"),
                    requireText(item == null ? null : item.targetSubsystem(), "project demo step target subsystem is required"),
                    normalizeOptionalText(item == null ? null : item.actionKey()),
                    normalizeEnabled(item == null ? null : item.enabled())
            ));
        }

        JsonNode fallbackSnapshot = existingProfile == null
                ? buildFallbackMockSnapshot(project)
                : parseJson(existingProfile.getMockSnapshotJson(), buildFallbackMockSnapshot(project));
        JsonNode mockSnapshot = request == null || request.mockSnapshot() == null || request.mockSnapshot().isNull()
                ? fallbackSnapshot
                : request.mockSnapshot();

        return new NormalizedDemoInput(
                requireText(request == null ? null : request.overviewTitle(), "project demo overview title is required"),
                requireText(request == null ? null : request.overviewBody(), "project demo overview body is required"),
                normalizeBridgeMode(
                        request == null ? null : request.bridgeMode(),
                        existingProfile == null ? null : existingProfile.getBridgeMode()
                ),
                normalizeEnabled(request == null ? null : request.enabled()),
                steps,
                normalizeMockSnapshot(mockSnapshot)
        );
    }

    private NormalizedRuntimeInput normalizeRuntimeRequest(
            ClubAppWorkspaceProjectDemoRuntimeRequest request,
            ClubAppWorkspaceProjectEntity project
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        String sessionId = normalizeText(request == null ? null : request.sessionId());
        if (sessionId.isEmpty()) {
            sessionId = project.getProjectKey() + "-" + UUID.randomUUID();
        }
        payload.put("sessionId", sessionId);
        payload.set("watch", normalizeNode(request == null ? null : request.watch(), objectMapper.createObjectNode()));
        payload.set("helmet", normalizeNode(request == null ? null : request.helmet(), objectMapper.createObjectNode()));
        payload.set("app", normalizeNode(request == null ? null : request.app(), objectMapper.createObjectNode()));
        payload.set("alerts", normalizeArrayNode(request == null ? null : request.alerts()));
        payload.set("recommendation", normalizeNode(request == null ? null : request.recommendation(), objectMapper.createObjectNode()));
        payload.put("updatedAt", Instant.now().toString());

        List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> rawEvents =
                request == null || request.events() == null ? List.of() : request.events();
        List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> normalizedEvents = new ArrayList<>();
        for (ClubAppWorkspaceProjectDemoRuntimeEventRequest event : rawEvents) {
            String title = requireText(event == null ? null : event.title(), "project demo event title is required");
            normalizedEvents.add(new ClubAppWorkspaceProjectDemoRuntimeEventRequest(
                    normalizeMode(event == null ? null : event.eventType(), "project demo event type is required"),
                    title,
                    normalizeText(event == null ? null : event.detail()),
                    normalizeMode(event == null ? null : event.level(), "project demo event level is required"),
                    event == null || event.eventAt() == null ? Instant.now() : event.eventAt()
            ));
        }

        return new NormalizedRuntimeInput(
                sessionId,
                normalizeMode(request == null ? null : request.sourceType(), "project demo source type is required"),
                payload,
                normalizedEvents
        );
    }

    private NormalizedRiskSyncInput normalizeRiskSyncRequest(
            ClubAppWorkspaceProjectDemoRiskSyncRequest request,
            ClubAppWorkspaceProjectEntity project
    ) {
        String sessionId = normalizeText(request == null ? null : request.sessionId());
        if (sessionId.isEmpty()) {
            sessionId = project.getProjectKey() + "-risk-sync";
        }

        String sourceType = normalizeText(request == null ? null : request.sourceType()).toUpperCase();
        if (sourceType.isEmpty()) {
            sourceType = "MHV";
        }

        JsonNode riskDemo = normalizeNode(
                request == null ? null : request.riskDemo(),
                objectMapper.createObjectNode()
        );
        if (!riskDemo.isObject()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "project demo risk sync payload must be an object");
        }

        List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> rawEvents =
                request == null || request.events() == null ? List.of() : request.events();
        List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> normalizedEvents = new ArrayList<>();
        for (ClubAppWorkspaceProjectDemoRuntimeEventRequest event : rawEvents) {
            String title = requireText(event == null ? null : event.title(), "project demo event title is required");
            normalizedEvents.add(new ClubAppWorkspaceProjectDemoRuntimeEventRequest(
                    normalizeMode(event == null ? null : event.eventType(), "project demo event type is required"),
                    title,
                    normalizeText(event == null ? null : event.detail()),
                    normalizeMode(event == null ? null : event.level(), "project demo event level is required"),
                    event == null || event.eventAt() == null ? Instant.now() : event.eventAt()
            ));
        }

        return new NormalizedRiskSyncInput(
                sessionId,
                sourceType,
                (ObjectNode) riskDemo,
                normalizedEvents
        );
    }

    private ObjectNode mergeRiskRuntimePayload(JsonNode currentPayload, NormalizedRiskSyncInput input) {
        String now = Instant.now().toString();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("sessionId", input.sessionId());
        payload.set("watch", readObject(currentPayload, "watch"));
        payload.set("helmet", readObject(currentPayload, "helmet"));

        ObjectNode mergedRiskDemo = mergeRiskDemo(
                readObject(readObject(currentPayload, "app"), "qingyuRisk"),
                input.riskDemo(),
                now
        );
        ObjectNode recommendation = buildRiskRecommendation(mergedRiskDemo);
        ObjectNode app = ensureObjectNode(readObject(currentPayload, "app"));
        app.put("navigationHint", "查看风险处理");
        app.put("recommendationText", readText(recommendation.get("body"), ""));
        app.put("lastSyncTime", now);
        app.set("qingyuRisk", mergedRiskDemo);

        payload.set("app", app);
        payload.set("alerts", buildRiskSummaryAlerts(mergedRiskDemo));
        payload.set("recommendation", recommendation);
        payload.put("updatedAt", now);
        return payload;
    }

    private ObjectNode mergeRiskDemo(JsonNode existingRiskDemo, ObjectNode incomingRiskDemo, String now) {
        ObjectNode mergedRiskDemo = ensureObjectNode(incomingRiskDemo.deepCopy());
        ArrayNode mergedAlerts = objectMapper.createArrayNode();
        Map<String, ObjectNode> existingAlertMap = new LinkedHashMap<>();

        JsonNode existingAlerts = existingRiskDemo == null ? null : existingRiskDemo.get("alerts");
        if (existingAlerts != null && existingAlerts.isArray()) {
            for (JsonNode node : existingAlerts) {
                if (!node.isObject()) {
                    continue;
                }
                String warningCode = readText(node.get("warningCode"), "");
                if (!warningCode.isEmpty()) {
                    existingAlertMap.put(warningCode, ensureObjectNode(node));
                }
            }
        }

        JsonNode incomingAlerts = incomingRiskDemo.get("alerts");
        if (incomingAlerts != null && incomingAlerts.isArray()) {
            for (JsonNode node : incomingAlerts) {
                if (!node.isObject()) {
                    continue;
                }
                ObjectNode incomingAlert = ensureObjectNode(node.deepCopy());
                String warningCode = readText(incomingAlert.get("warningCode"), "");
                if (!warningCode.isEmpty()) {
                    ObjectNode existingAlert = existingAlertMap.get(warningCode);
                    if (existingAlert != null) {
                        preserveAlertField(existingAlert, incomingAlert, "status");
                        preserveAlertField(existingAlert, incomingAlert, "statusNote");
                        preserveAlertField(existingAlert, incomingAlert, "handledAt");
                        preserveAlertField(existingAlert, incomingAlert, "handledBy");
                        preserveAlertField(existingAlert, incomingAlert, "statusUpdatedAt");
                    }
                }
                if (readText(incomingAlert.get("status"), "").isEmpty()) {
                    incomingAlert.put("status", "待处理");
                }
                mergedAlerts.add(incomingAlert);
            }
        }

        mergedRiskDemo.set("alerts", mergedAlerts);
        mergedRiskDemo.put("totalCount", mergedAlerts.size());
        mergedRiskDemo.put("resolvedCount", countResolvedAlerts(mergedAlerts));
        mergedRiskDemo.put("openCount", countOpenAlerts(mergedAlerts));
        mergedRiskDemo.put("highRiskCount", countHighRiskOpenAlerts(mergedAlerts));
        mergedRiskDemo.put("updatedAt", now);
        return mergedRiskDemo;
    }

    private ArrayNode buildRiskSummaryAlerts(ObjectNode riskDemo) {
        ArrayNode alerts = objectMapper.createArrayNode();
        JsonNode sourceAlerts = riskDemo.get("alerts");
        if (sourceAlerts == null || !sourceAlerts.isArray()) {
            return alerts;
        }

        for (JsonNode node : sourceAlerts) {
            if (!node.isObject()) {
                continue;
            }
            if ("已结案".equals(readText(node.get("status"), ""))) {
                continue;
            }
            ObjectNode alert = alerts.addObject();
            alert.put("level", readText(node.get("severity"), "INFO"));
            alert.put("title", readText(node.get("summary"), "风险记录"));
            alert.put(
                    "detail",
                    readText(
                            node.get("reason"),
                            readText(node.get("transcriptExcerpt"), readText(node.get("warningCode"), ""))
                    )
            );
            if (alerts.size() >= 3) {
                break;
            }
        }
        return alerts;
    }

    private ObjectNode buildRiskRecommendation(ObjectNode riskDemo) {
        ObjectNode recommendation = objectMapper.createObjectNode();
        int openCount = readInt(riskDemo.get("openCount"));
        int highRiskCount = readInt(riskDemo.get("highRiskCount"));
        if (openCount <= 0) {
            recommendation.put("level", "INFO");
            recommendation.put("title", "当前无待处理风险");
            recommendation.put("body", "当前没有待处理风险记录，可继续关注新的语音分析结果。");
            return recommendation;
        }
        if (highRiskCount > 0) {
            recommendation.put("level", "HIGH");
            recommendation.put("title", "存在高危风险");
            recommendation.put("body", "当前仍有高危风险记录待处理，请优先查看最新记录并确认跟进状态。");
            return recommendation;
        }
        recommendation.put("level", "WARNING");
        recommendation.put("title", "存在待跟进风险");
        recommendation.put("body", "当前存在待处理或跟进中的风险记录，请及时查看并更新处理状态。");
        return recommendation;
    }

    private void preserveAlertField(ObjectNode existingAlert, ObjectNode incomingAlert, String fieldName) {
        JsonNode existingValue = existingAlert.get(fieldName);
        if (existingValue == null || existingValue.isNull()) {
            return;
        }
        String incomingText = readText(incomingAlert.get(fieldName), "");
        if (!incomingText.isEmpty()) {
            return;
        }
        incomingAlert.set(fieldName, existingValue);
    }

    private int countResolvedAlerts(ArrayNode alerts) {
        int count = 0;
        for (JsonNode alert : alerts) {
            if ("已结案".equals(readText(alert.get("status"), ""))) {
                count++;
            }
        }
        return count;
    }

    private int countOpenAlerts(ArrayNode alerts) {
        int count = 0;
        for (JsonNode alert : alerts) {
            if (!"已结案".equals(readText(alert.get("status"), ""))) {
                count++;
            }
        }
        return count;
    }

    private int countHighRiskOpenAlerts(ArrayNode alerts) {
        int count = 0;
        for (JsonNode alert : alerts) {
            if ("已结案".equals(readText(alert.get("status"), ""))) {
                continue;
            }
            if ("高危".equals(readText(alert.get("severity"), ""))) {
                count++;
            }
        }
        return count;
    }

    private ClubAppWorkspaceProjectDemoEventEntity toEventEntity(
            Long projectId,
            String sessionId,
            ClubAppWorkspaceProjectDemoRuntimeEventRequest event
    ) {
        ClubAppWorkspaceProjectDemoEventEntity entity = new ClubAppWorkspaceProjectDemoEventEntity();
        entity.setProjectId(projectId);
        entity.setSourceSessionId(sessionId);
        entity.setEventType(event.eventType());
        entity.setTitle(event.title());
        entity.setDetail(event.detail());
        entity.setLevel(event.level());
        entity.setEventAt(event.eventAt() == null ? Instant.now() : event.eventAt());
        return entity;
    }

    private void trimEventHistory(Long projectId) {
        List<ClubAppWorkspaceProjectDemoEventEntity> events =
                eventRepository.findAllByProjectIdOrderByEventAtDescIdDesc(projectId);
        if (events.size() <= MAX_EVENT_HISTORY) {
            return;
        }
        List<Long> staleIds = events.subList(MAX_EVENT_HISTORY, events.size()).stream()
                .map(ClubAppWorkspaceProjectDemoEventEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!staleIds.isEmpty()) {
            eventRepository.deleteAllByIdInBatch(staleIds);
        }
    }

    private ClubAppWorkspaceProjectDemoStepResponse toVisibleStepResponse(ClubAppWorkspaceProjectDemoStepEntity entity) {
        return new ClubAppWorkspaceProjectDemoStepResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getTriggerType(),
                entity.getTargetSubsystem(),
                entity.getActionKey(),
                entity.getSortOrder()
        );
    }

    private ManagerClubAppWorkspaceProjectDemoStepResponse toManagerStepResponse(ClubAppWorkspaceProjectDemoStepEntity entity) {
        return new ManagerClubAppWorkspaceProjectDemoStepResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getTriggerType(),
                entity.getTargetSubsystem(),
                entity.getActionKey(),
                entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getEnabled())
        );
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

    private ClubAppWorkspaceProjectEntity getRequiredProject(Long clubId, String projectKey) {
        List<Long> groupIds = groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(clubId).stream()
                .map(ClubAppWorkspaceGroupEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (groupIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "workspace project not found");
        }
        return projectRepository.findFirstByGroupIdInAndProjectKey(groupIds, normalizeProjectKey(projectKey))
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

    private AuthenticatedUser requireManagerAccessToClub(Long clubId) {
        getRequiredClub(clubId);
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager role required");
        }
        if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager permission denied");
        }
        return user;
    }

    private Map<Long, ClubAppWorkspaceProjectDemoStepEntity> toStepMap(List<ClubAppWorkspaceProjectDemoStepEntity> entities) {
        Map<Long, ClubAppWorkspaceProjectDemoStepEntity> result = new LinkedHashMap<>();
        for (ClubAppWorkspaceProjectDemoStepEntity entity : entities) {
            result.put(entity.getId(), entity);
        }
        return result;
    }

    private ClubAppWorkspaceProjectDemoStepEntity requireExistingStep(
            Map<Long, ClubAppWorkspaceProjectDemoStepEntity> existingStepMap,
            Long stepId
    ) {
        ClubAppWorkspaceProjectDemoStepEntity entity = existingStepMap.get(stepId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "project demo step is invalid");
        }
        return entity;
    }

    private void deleteStaleSteps(List<ClubAppWorkspaceProjectDemoStepEntity> existingSteps, Set<Long> keptStepIds) {
        List<Long> staleIds = existingSteps.stream()
                .map(ClubAppWorkspaceProjectDemoStepEntity::getId)
                .filter(existingId -> !keptStepIds.contains(existingId))
                .toList();
        if (!staleIds.isEmpty()) {
            stepRepository.deleteAllByIdInBatch(staleIds);
        }
    }

    private Long normalizeId(Long value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private boolean normalizeEnabled(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeMode(String value, String message) {
        String normalized = normalizeText(value).toUpperCase();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return normalized;
    }

    private String normalizeBridgeMode(String value, String fallbackValue) {
        String normalized = normalizeText(value).toUpperCase();
        if (!normalized.isEmpty()) {
            return normalized;
        }

        String fallback = normalizeText(fallbackValue).toUpperCase();
        return fallback.isEmpty() ? DEFAULT_BRIDGE_MODE : fallback;
    }

    private String normalizeProjectKey(String value) {
        String normalized = normalizeText(value).toLowerCase();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project key is invalid");
        }
        return normalized;
    }

    private JsonNode normalizeMockSnapshot(JsonNode node) {
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("sessionId", readText(node.get("sessionId"), "mock-session"));
        normalized.set("watch", normalizeNode(node.get("watch"), objectMapper.createObjectNode()));
        normalized.set("helmet", normalizeNode(node.get("helmet"), objectMapper.createObjectNode()));
        normalized.set("app", normalizeNode(node.get("app"), objectMapper.createObjectNode()));
        normalized.set("alerts", normalizeArrayNode(node.get("alerts")));
        normalized.set("recommendation", normalizeNode(node.get("recommendation"), objectMapper.createObjectNode()));
        return normalized;
    }

    private JsonNode normalizeNode(JsonNode node, JsonNode fallback) {
        if (node == null || node.isNull()) {
            return fallback;
        }
        return node;
    }

    private ArrayNode normalizeArrayNode(JsonNode node) {
        if (node != null && node.isArray()) {
            return (ArrayNode) node;
        }
        return objectMapper.createArrayNode();
    }

    private JsonNode parseJson(String rawJson, JsonNode fallback) {
        if (rawJson == null || rawJson.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            return fallback;
        }
    }

    private String writeJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize project demo json failed", exception);
        }
    }

    private ObjectNode buildFallbackMockSnapshot(ClubAppWorkspaceProjectEntity project) {
        if ("cycling-guardian".equals(project.getProjectKey())) {
            return buildCyclingGuardianMockSnapshot();
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("sessionId", project.getProjectKey() + "-mock");
        root.set("watch", objectMapper.createObjectNode());
        root.set("helmet", objectMapper.createObjectNode());
        ObjectNode app = objectMapper.createObjectNode();
        app.put("navigationHint", "演示待配置");
        app.put("recommendationText", "当前项目尚未配置实时演示数据。");
        app.put("lastSyncTime", "--");
        root.set("app", app);
        root.set("alerts", objectMapper.createArrayNode());
        ObjectNode recommendation = objectMapper.createObjectNode();
        recommendation.put("level", "INFO");
        recommendation.put("title", "待配置");
        recommendation.put("body", "教师端可在 Demo 配置页补充步骤和默认快照。");
        root.set("recommendation", recommendation);
        return root;
    }

    private ObjectNode buildCyclingGuardianMockSnapshot() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("sessionId", "cycling-guardian-demo");

        ObjectNode watch = objectMapper.createObjectNode();
        watch.put("online", false);
        watch.put("bleState", "待连接");
        watch.put("heartRate", 86);
        watch.put("spo2", 98);
        watch.put("screenState", "待机");
        watch.put("recentReminder", "注意右后侧来车");
        root.set("watch", watch);

        ObjectNode helmet = objectMapper.createObjectNode();
        helmet.put("online", false);
        helmet.put("cameraStatus", "离线");
        helmet.put("targetCount", 2);
        helmet.put("riskLevel", "MEDIUM");
        helmet.put("distanceAlert", "前方 2.6 米");
        helmet.put("posture", "姿态稳定");
        helmet.put("gps", "固原二中测试路线");
        helmet.put("lightMode", "紫色呼吸");
        root.set("helmet", helmet);

        ObjectNode app = objectMapper.createObjectNode();
        app.put("navigationHint", "前方路口减速直行");
        app.put("recommendationText", "检测到右后方来车，建议保持车距并降低速度。");
        app.put("lastSyncTime", "--");
        root.set("app", app);

        ArrayNode alerts = objectMapper.createArrayNode();
        alerts.addObject()
                .put("level", "WARNING")
                .put("title", "右后方来车")
                .put("detail", "建议减速并保持当前车道");
        alerts.addObject()
                .put("level", "INFO")
                .put("title", "腕表提醒")
                .put("detail", "当前心率稳定，腕表已同步风险提示");
        root.set("alerts", alerts);

        ObjectNode recommendation = objectMapper.createObjectNode();
        recommendation.put("level", "MEDIUM");
        recommendation.put("title", "建议减速观察");
        recommendation.put("body", "系统综合视觉识别、距离检测和腕表状态后，建议降低速度并关注右侧后方车辆。");
        root.set("recommendation", recommendation);
        return root;
    }

    private String fallbackOverviewTitle(ClubAppWorkspaceProjectEntity project) {
        String title = normalizeText(project.getTitle());
        return title.isEmpty() ? "项目 Demo" : title + " Demo";
    }

    private String fallbackOverviewBody(ClubAppWorkspaceProjectEntity project) {
        String summary = normalizeText(project.getSummary());
        if (!summary.isEmpty()) {
            return summary;
        }
        String subtitle = normalizeText(project.getSubtitle());
        return subtitle.isEmpty() ? "当前项目暂未配置演示内容。" : subtitle;
    }

    private String readText(JsonNode node, String fallback) {
        if (node == null || node.isNull()) {
            return fallback;
        }
        String value = node.asText("").trim();
        return value.isEmpty() ? fallback : value;
    }

    private JsonNode readObject(JsonNode payload, String fieldName) {
        JsonNode node = payload == null ? null : payload.get(fieldName);
        return node != null && node.isObject() ? node : objectMapper.createObjectNode();
    }

    private JsonNode readArray(JsonNode payload, String fieldName) {
        JsonNode node = payload == null ? null : payload.get(fieldName);
        return node != null && node.isArray() ? node : objectMapper.createArrayNode();
    }

    private ObjectNode ensureObjectNode(JsonNode node) {
        return node != null && node.isObject()
                ? (ObjectNode) node
                : objectMapper.createObjectNode();
    }

    private int readInt(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        String value = node.asText("").trim();
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private ClubAppWorkspaceProjectDemoStepEntity buildStep(
            Long profileId,
            String title,
            String description,
            String triggerType,
            String targetSubsystem,
            String actionKey,
            int sortOrder,
            Long operatorUserId
    ) {
        ClubAppWorkspaceProjectDemoStepEntity entity = new ClubAppWorkspaceProjectDemoStepEntity();
        entity.setDemoProfileId(profileId);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setTriggerType(triggerType);
        entity.setTargetSubsystem(targetSubsystem);
        entity.setActionKey(actionKey);
        entity.setSortOrder(sortOrder);
        entity.setEnabled(true);
        entity.setCreatedBy(operatorUserId);
        entity.setUpdatedBy(operatorUserId);
        return entity;
    }

    private record NormalizedDemoInput(
            String overviewTitle,
            String overviewBody,
            String bridgeMode,
            boolean enabled,
            List<NormalizedStepInput> steps,
            JsonNode mockSnapshot
    ) {
    }

    private record NormalizedStepInput(
            Long id,
            String title,
            String description,
            String triggerType,
            String targetSubsystem,
            String actionKey,
            boolean enabled
    ) {
    }

    private record NormalizedRuntimeInput(
            String sessionId,
            String sourceType,
            JsonNode payload,
            List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> events
    ) {
    }

    private record NormalizedRiskSyncInput(
            String sessionId,
            String sourceType,
            ObjectNode riskDemo,
            List<ClubAppWorkspaceProjectDemoRuntimeEventRequest> events
    ) {
    }
}


