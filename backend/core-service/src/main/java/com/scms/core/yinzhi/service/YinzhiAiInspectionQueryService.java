package com.scms.core.yinzhi.service;

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
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import com.scms.core.yinzhi.domain.YinzhiAiDeviceCommandEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionLogEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionRunEntity;
import com.scms.core.yinzhi.dto.YinzhiAiDeviceCommandResponse;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionLogResponse;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionResponse;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionRunResponse;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionRunSummaryResponse;
import com.scms.core.yinzhi.dto.YinzhiDeviceCommandAckRequest;
import com.scms.core.yinzhi.dto.YinzhiDeviceCommandPollResponse;
import com.scms.core.yinzhi.dto.YinzhiManualConfirmRequest;
import com.scms.core.yinzhi.repository.YinzhiAiDeviceCommandRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionLogRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class YinzhiAiInspectionQueryService {

    private static final String PROJECT_KEY = "yinzhi-guanjia";
    private static final String BLOCKED_HIGH_RISK_COMMAND = "DOOR_GARAGE_CONTROL_BLOCKED";
    private static final Duration RECENT_VISIBLE_COMMAND_WINDOW = Duration.ofMinutes(2);
    private static final long RECENT_VISIBLE_COMMAND_UPLOAD_SEQUENCE_WINDOW = 8L;
    private static final List<String> RECENT_VISIBLE_COMMAND_STATUSES =
            List.of("QUEUED", "DELIVERED", "ACKED", "VERIFIED", "FAILED");

    private final YinzhiAiInspectionRunRepository runRepository;
    private final YinzhiAiInspectionLogRepository logRepository;
    private final YinzhiAiDeviceCommandRepository commandRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubAppWorkspaceGroupRepository groupRepository;
    private final ClubAppWorkspaceProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public YinzhiAiInspectionQueryService(
            YinzhiAiInspectionRunRepository runRepository,
            YinzhiAiInspectionLogRepository logRepository,
            YinzhiAiDeviceCommandRepository commandRepository,
            ClubRepository clubRepository,
            ClubManagerBindingRepository clubManagerBindingRepository,
            ClubStudentMemberRepository clubStudentMemberRepository,
            ClubAppWorkspaceGroupRepository groupRepository,
            ClubAppWorkspaceProjectRepository projectRepository,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.runRepository = runRepository;
        this.logRepository = logRepository;
        this.commandRepository = commandRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.groupRepository = groupRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public YinzhiAiInspectionResponse getVisibleLatest(Long clubId, String projectKey) {
        requireViewerAccessToClub(clubId);
        getRequiredVisibleProject(clubId, projectKey);
        return buildLatestResponse();
    }

    @Transactional
    public YinzhiAiInspectionResponse confirmHighRiskManualHandling(
            Long clubId,
            String projectKey,
            Long commandId,
            YinzhiManualConfirmRequest request
    ) {
        AuthenticatedUser user = requireViewerAccessToClub(clubId);
        getRequiredVisibleProject(clubId, projectKey);
        YinzhiAiDeviceCommandEntity command = commandRepository.findById(commandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "device command not found"));
        if (!isHighRiskManualCommand(command)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "only blocked high risk command can be manually confirmed");
        }
        if ("MANUAL_CONFIRMED".equals(normalizeText(command.getStatus()).toUpperCase())) {
            return buildLatestResponse();
        }

        String note = normalizeText(request == null ? null : request.note());
        String actor = normalizeText(user.username()).isEmpty()
                ? "userId=" + user.userId()
                : user.username();
        String message = "App 端已确认高风险动作并授权自动执行关闭动作，确认人=" + actor
                + (note.isEmpty() ? "" : "，备注=" + note);
        command.setStatus("MANUAL_CONFIRMED");
        command.setAckedAt(Instant.now());
        command.setAckMessage(message);
        commandRepository.save(command);
        appendLog(command, "MANUAL_CONFIRMED", "WARNING", message);

        runRepository.findById(command.getRunId()).ifPresent(run -> {
            int queuedCount = queueAuthorizedHighRiskCommands(run, command);
            run.setStatus(queuedCount > 0 ? "COMMAND_QUEUED" : "MANUAL_CONFIRMED");
            runRepository.save(run);
        });
        return buildLatestResponse();
    }

    @Transactional(readOnly = true)
    public YinzhiAiInspectionResponse getObserverLatest() {
        return buildLatestResponse();
    }

    @Transactional
    public YinzhiDeviceCommandPollResponse pollNextCommand(String deviceId) {
        String normalizedDeviceId = normalizeText(deviceId);
        if (normalizedDeviceId.isEmpty()) {
            return YinzhiDeviceCommandPollResponse.empty();
        }
        return commandRepository.findFirstByDeviceIdAndStatusOrderByQueuedAtAscIdAsc(normalizedDeviceId, "QUEUED")
                .map(command -> {
                    command.setStatus("DELIVERED");
                    command.setDeliveredAt(Instant.now());
                    commandRepository.save(command);
                    appendLog(command, "COMMAND_DELIVERED", "INFO",
                            "ESP32 已轮询到 AI 设备命令，commandId=" + command.getId()
                                    + "，commandType=" + command.getCommandType());
                    return new YinzhiDeviceCommandPollResponse(
                            true,
                            command.getId(),
                            command.getRunId(),
                            command.getTelemetryEventId(),
                            command.getUploadSequence(),
                            command.getCommandType(),
                            command.getRiskLevel(),
                            command.getDetail()
                    );
                })
                .orElseGet(YinzhiDeviceCommandPollResponse::empty);
    }

    @Transactional
    public YinzhiDeviceCommandPollResponse ackCommand(Long commandId, YinzhiDeviceCommandAckRequest request) {
        YinzhiAiDeviceCommandEntity command = commandRepository.findById(commandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "device command not found"));
        String status = normalizeText(request == null ? null : request.status()).toUpperCase();
        String ackMessage = normalizeText(request == null ? null : request.message());
        command.setStatus("FAILED".equals(status) ? "FAILED" : "ACKED");
        command.setAckedAt(Instant.now());
        command.setAckMessage(ackMessage);
        commandRepository.save(command);
        appendLog(command, "ACKED", "INFO",
                "ESP32 已回传命令执行结果，commandId=" + command.getId()
                        + "，status=" + command.getStatus()
                        + (ackMessage.isEmpty() ? "" : "，message=" + ackMessage));

        runRepository.findById(command.getRunId()).ifPresent(run -> {
            run.setStatus(command.getStatus());
            runRepository.save(run);
        });

        return new YinzhiDeviceCommandPollResponse(
                true,
                command.getId(),
                command.getRunId(),
                command.getTelemetryEventId(),
                command.getUploadSequence(),
                command.getCommandType(),
                command.getRiskLevel(),
                command.getDetail()
        );
    }

    private YinzhiAiInspectionResponse buildLatestResponse() {
        YinzhiAiInspectionRunEntity latest = runRepository.findFirstByOrderByCreatedAtDescIdDesc().orElse(null);
        if (latest == null) {
            return new YinzhiAiInspectionResponse(Instant.now(), false, null, List.of(), List.of(), List.of());
        }
        List<YinzhiAiInspectionLogResponse> logs = logRepository.findAllByRunIdOrderByCreatedAtAscIdAsc(latest.getId()).stream()
                .map(this::toLogResponse)
                .toList();
        List<YinzhiAiDeviceCommandEntity> commandEntities = new ArrayList<>(
                commandRepository.findAllByRunIdOrderByQueuedAtAscIdAsc(latest.getId())
        );
        if (latest.getTelemetryEventId() != null) {
            for (YinzhiAiDeviceCommandEntity command : commandRepository
                    .findAllByVerificationTelemetryEventIdOrderByQueuedAtAscIdAsc(latest.getTelemetryEventId())) {
                if (!containsCommand(commandEntities, command)) {
                    commandEntities.add(command);
                }
            }
        }
        String latestDeviceId = normalizeText(latest.getDeviceId());
        if (!latestDeviceId.isEmpty()) {
            List<YinzhiAiDeviceCommandEntity> recentDeviceCommands =
                    commandRepository.findTop20ByDeviceIdAndStatusInOrderByQueuedAtDescIdDesc(
                            latestDeviceId,
                            RECENT_VISIBLE_COMMAND_STATUSES
                    );
            if (recentDeviceCommands != null) {
                Set<String> includedRecentCommandTypes = new HashSet<>();
                for (YinzhiAiDeviceCommandEntity command : recentDeviceCommands) {
                    String commandType = normalizeText(command.getCommandType()).toUpperCase();
                    if (isRecentCommandVisibleBesideLatestRun(command, latest)
                            && includedRecentCommandTypes.add(commandType)
                            && !containsCommand(commandEntities, command)) {
                        commandEntities.add(command);
                    }
                }
            }
        }
        List<YinzhiAiDeviceCommandResponse> commands = commandEntities.stream()
                .map(this::toCommandResponse)
                .toList();
        List<YinzhiAiInspectionRunSummaryResponse> recentRuns = runRepository.findTop10ByOrderByCreatedAtDescIdDesc().stream()
                .map(this::toRunSummaryResponse)
                .toList();
        return new YinzhiAiInspectionResponse(
                Instant.now(),
                true,
                toRunResponse(latest),
                recentRuns,
                logs,
                commands
        );
    }

    private boolean containsCommand(List<YinzhiAiDeviceCommandEntity> commandEntities, YinzhiAiDeviceCommandEntity command) {
        if (command == null || command.getId() == null) {
            return false;
        }
        return commandEntities.stream()
                .anyMatch(existing -> Objects.equals(existing.getId(), command.getId()));
    }

    private boolean isRecentCommandVisibleBesideLatestRun(
            YinzhiAiDeviceCommandEntity command,
            YinzhiAiInspectionRunEntity latest
    ) {
        if (command == null || latest == null) {
            return false;
        }
        if (BLOCKED_HIGH_RISK_COMMAND.equals(normalizeText(command.getCommandType()).toUpperCase())) {
            return false;
        }

        String status = normalizeText(command.getStatus()).toUpperCase();
        if ("QUEUED".equals(status) || "DELIVERED".equals(status)) {
            return true;
        }

        Instant queuedAt = command.getQueuedAt();
        Instant latestCreatedAt = latest.getCreatedAt();
        if (queuedAt != null && latestCreatedAt != null) {
            return !queuedAt.isBefore(latestCreatedAt.minus(RECENT_VISIBLE_COMMAND_WINDOW));
        }

        Long commandUploadSequence = command.getUploadSequence();
        Long latestUploadSequence = latest.getUploadSequence();
        if (commandUploadSequence == null || latestUploadSequence == null) {
            return false;
        }
        return commandUploadSequence >= latestUploadSequence - RECENT_VISIBLE_COMMAND_UPLOAD_SEQUENCE_WINDOW;
    }

    private void appendLog(YinzhiAiDeviceCommandEntity command, String stage, String level, String message) {
        YinzhiAiInspectionLogEntity logEntry = new YinzhiAiInspectionLogEntity();
        logEntry.setRunId(command.getRunId());
        logEntry.setTelemetryEventId(command.getTelemetryEventId());
        logEntry.setDeviceId(command.getDeviceId());
        logEntry.setUploadSequence(command.getUploadSequence());
        logEntry.setStage(stage);
        logEntry.setLevel(level);
        logEntry.setMessage(message);
        logRepository.save(logEntry);
    }

    private YinzhiAiInspectionRunResponse toRunResponse(YinzhiAiInspectionRunEntity run) {
        return new YinzhiAiInspectionRunResponse(
                run.getId(),
                run.getTelemetryEventId(),
                run.getDeviceId(),
                run.getUploadSequence(),
                run.getTriggerReason(),
                run.getOwnerPresence(),
                run.getSceneLabel(),
                run.getRiskLevel(),
                run.getPermissionDecision(),
                run.getActionSummary(),
                run.getAiProvider(),
                run.getAiModel(),
                run.getAiFinishReason(),
                safeInt(run.getAiPromptTokens()),
                safeInt(run.getAiCompletionTokens()),
                safeInt(run.getAiTotalTokens()),
                run.getAiReport(),
                Boolean.TRUE.equals(run.getFallbackReport()),
                run.getStatus(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                parseJson(run.getRawTelemetryJson()),
                parseJson(run.getDecisionJson())
        );
    }

    private YinzhiAiInspectionRunSummaryResponse toRunSummaryResponse(YinzhiAiInspectionRunEntity run) {
        return new YinzhiAiInspectionRunSummaryResponse(
                run.getId(),
                run.getTelemetryEventId(),
                run.getDeviceId(),
                run.getUploadSequence(),
                run.getSceneLabel(),
                run.getRiskLevel(),
                run.getStatus(),
                run.getCreatedAt()
        );
    }

    private YinzhiAiInspectionLogResponse toLogResponse(YinzhiAiInspectionLogEntity log) {
        return new YinzhiAiInspectionLogResponse(
                log.getId(),
                log.getStage(),
                log.getLevel(),
                log.getMessage(),
                log.getCreatedAt(),
                parseJson(log.getRawJson())
        );
    }

    private YinzhiAiDeviceCommandResponse toCommandResponse(YinzhiAiDeviceCommandEntity command) {
        return new YinzhiAiDeviceCommandResponse(
                command.getId(),
                command.getCommandType(),
                command.getRiskLevel(),
                command.getStatus(),
                command.getDetail(),
                command.getQueuedAt(),
                command.getDeliveredAt(),
                command.getAckedAt(),
                command.getAckMessage(),
                command.getVerifiedAt(),
                command.getVerificationTelemetryEventId(),
                command.getVerificationUploadSequence(),
                command.getVerificationMessage()
        );
    }

    private boolean isHighRiskManualCommand(YinzhiAiDeviceCommandEntity command) {
        if (command == null) {
            return false;
        }
        String riskLevel = normalizeText(command.getRiskLevel()).toUpperCase();
        String commandType = normalizeText(command.getCommandType()).toUpperCase();
        String status = normalizeText(command.getStatus()).toUpperCase();
        return "HIGH".equals(riskLevel)
                && BLOCKED_HIGH_RISK_COMMAND.equals(commandType)
                && ("BLOCKED".equals(status) || "MANUAL_CONFIRMED".equals(status));
    }

    private int queueAuthorizedHighRiskCommands(
            YinzhiAiInspectionRunEntity run,
            YinzhiAiDeviceCommandEntity blockedCommand
    ) {
        if (run == null || blockedCommand == null) {
            return 0;
        }
        JsonNode rawTelemetry = parseJson(run.getRawTelemetryJson());
        int queuedCount = 0;
        if (Boolean.TRUE.equals(boolAt(rawTelemetry, "atmega", "door_open"))) {
            YinzhiAiDeviceCommandEntity closeDoor = buildAuthorizedCommand(
                    blockedCommand,
                    "DOOR_CLOSE",
                    "App 已人工确认，授权 AI 自动关闭已开启的房门。"
            );
            commandRepository.save(closeDoor);
            appendLog(closeDoor, "AUTHORIZED_COMMAND_QUEUED", "WARNING",
                    "App 确认后，高风险关门命令已入队，等待 ESP32 轮询执行：commandType=DOOR_CLOSE");
            queuedCount++;
        }
        if (Boolean.TRUE.equals(boolAt(rawTelemetry, "atmega", "garage_open"))) {
            YinzhiAiDeviceCommandEntity closeGarage = buildAuthorizedCommand(
                    blockedCommand,
                    "GARAGE_CLOSE",
                    "App 已人工确认，授权 AI 自动关闭已开启的车库门。"
            );
            commandRepository.save(closeGarage);
            appendLog(closeGarage, "AUTHORIZED_COMMAND_QUEUED", "WARNING",
                    "App 确认后，高风险关车库门命令已入队，等待 ESP32 轮询执行：commandType=GARAGE_CLOSE");
            queuedCount++;
        }
        return queuedCount;
    }

    private YinzhiAiDeviceCommandEntity buildAuthorizedCommand(
            YinzhiAiDeviceCommandEntity source,
            String commandType,
            String detail
    ) {
        YinzhiAiDeviceCommandEntity command = new YinzhiAiDeviceCommandEntity();
        command.setRunId(source.getRunId());
        command.setTelemetryEventId(source.getTelemetryEventId());
        command.setDeviceId(source.getDeviceId());
        command.setUploadSequence(source.getUploadSequence());
        command.setCommandType(commandType);
        command.setRiskLevel("HIGH");
        command.setStatus("QUEUED");
        command.setDetail(detail);
        return command;
    }

    private static Boolean boolAt(JsonNode root, String objectName, String fieldName) {
        JsonNode object = root == null ? null : root.get(objectName);
        JsonNode value = object == null ? null : object.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText("").trim().toLowerCase();
        if (text.isEmpty()) {
            return null;
        }
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    private JsonNode parseJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return JsonNodeFactory.instance.objectNode();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            return JsonNodeFactory.instance.objectNode();
        }
    }

    private void getRequiredVisibleProject(Long clubId, String projectKey) {
        if (!PROJECT_KEY.equals(normalizeProjectKey(projectKey))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "workspace project not found");
        }
        List<Long> groupIds = groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(clubId).stream()
                .map(ClubAppWorkspaceGroupEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (groupIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "workspace project not found");
        }
        ClubAppWorkspaceProjectEntity ignored = projectRepository
                .findFirstByGroupIdInAndProjectKeyAndEnabledTrue(groupIds, PROJECT_KEY)
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
        String normalized = normalizeText(value).toLowerCase();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project key is invalid");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
