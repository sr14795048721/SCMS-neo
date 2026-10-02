package com.scms.core.yinzhi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scms.core.ai.dto.AiCommitResponse;
import com.scms.core.ai.service.ScmsAiCompletionService;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import com.scms.core.yinzhi.domain.YinzhiAiDeviceCommandEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionLogEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionRunEntity;
import com.scms.core.yinzhi.repository.YinzhiAiDeviceCommandRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionLogRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class YinzhiAiInspectionService {

    private static final Logger log = LoggerFactory.getLogger(YinzhiAiInspectionService.class);
    private static final String PROJECT = "yinzhi_guanjia";
    private static final String AUX_HINT_ON_COMMAND = "AUX_HINT_ON";
    private static final String FAN_OFF_COMMAND = "FAN_OFF";
    private static final Duration AI_REPORT_DEDUP_WINDOW = Duration.ofMinutes(2);
    private static final List<String> CLOSED_LOOP_PENDING_STATUSES = List.of("DELIVERED", "ACKED");
    private static final String AI_REPORT_TEMPLATE_VERSION = "YINZHI_AI_INSPECTION_REPORT_V1";
    private static final String AI_REPORT_SYSTEM_PROMPT = """
            你是 SCMS 的 HomeMind AI 自主管家巡检模块。
            你必须严格按照用户给出的固定模板输出 Markdown 巡检报告。
            禁止更换标题、字段名和字段顺序，禁止新增小节、表格、代码块、寒暄或额外解释。
            不要声称云端可以自动开门或关闭车库门；门、车库门、布防解除等高风险动作只能写安全拦截或人工确认。
            若信息不足，必须写 UNKNOWN 或“未确认”，不要猜测。
            """.trim();

    private final YinzhiAiInspectionRunRepository runRepository;
    private final YinzhiAiInspectionLogRepository logRepository;
    private final YinzhiAiDeviceCommandRepository commandRepository;
    private final ScmsAiCompletionService aiCompletionService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public YinzhiAiInspectionService(
            YinzhiAiInspectionRunRepository runRepository,
            YinzhiAiInspectionLogRepository logRepository,
            YinzhiAiDeviceCommandRepository commandRepository,
            ScmsAiCompletionService aiCompletionService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.runRepository = runRepository;
        this.logRepository = logRepository;
        this.commandRepository = commandRepository;
        this.aiCompletionService = aiCompletionService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public Long inspectTelemetryEvent(IotDeviceTelemetryEventEntity telemetryEvent) {
        if (telemetryEvent == null || telemetryEvent.getId() == null) {
            return null;
        }
        if (!PROJECT.equals(telemetryEvent.getProject())) {
            return null;
        }

        JsonNode payload = parseJson(telemetryEvent.getPayloadJson());
        Snapshot snapshot = Snapshot.from(payload, telemetryEvent);
        Decision decision = decide(snapshot);

        FastInspectionResult result = transactionTemplate.execute(status -> {
            if (runRepository.existsByTelemetryEventId(telemetryEvent.getId())) {
                return null;
            }
            boolean enrichAiReport = shouldEnrichAiReport(snapshot, decision)
                    && !hasRecentSimilarAiReport(snapshot, decision);
            Long runId = persistFastInspection(telemetryEvent, payload, snapshot, decision, enrichAiReport);
            return new FastInspectionResult(runId, enrichAiReport);
        });
        if (result != null && result.runId() != null && result.enrichAiReport()) {
            enrichAiReport(result.runId(), snapshot, decision);
        }
        return result == null ? null : result.runId();
    }

    private Long persistFastInspection(IotDeviceTelemetryEventEntity telemetryEvent,
                                       JsonNode payload,
                                       Snapshot snapshot,
                                       Decision decision,
                                       boolean enrichAiReport) {
        YinzhiAiInspectionRunEntity run = new YinzhiAiInspectionRunEntity();
        run.setTelemetryEventId(telemetryEvent.getId());
        run.setDeviceId(telemetryEvent.getDeviceId());
        run.setUploadSequence(telemetryEvent.getUploadSequence());
        run.setTriggerReason(snapshot.triggerReason());
        run.setOwnerPresence(snapshot.ownerPresence());
        run.setSceneLabel(decision.sceneLabel());
        run.setRiskLevel(decision.riskLevel());
        run.setPermissionDecision(decision.permissionDecision());
        run.setActionSummary(decision.actionSummary());
        run.setRawTelemetryJson(telemetryEvent.getPayloadJson());
        run.setDecisionJson(writeJson(toDecisionJson(snapshot, decision)));
        run.setStatus("ANALYZED");

        AiResult aiResult = AiResult.fallback(buildFixedMarkdownReport(snapshot, decision));
        run.setAiProvider(aiResult.provider());
        run.setAiModel(aiResult.model());
        run.setAiFinishReason(aiResult.finishReason());
        run.setAiPromptTokens(aiResult.promptTokens());
        run.setAiCompletionTokens(aiResult.completionTokens());
        run.setAiTotalTokens(aiResult.totalTokens());
        run.setAiReport(aiResult.report());
        run.setFallbackReport(aiResult.fallback());
        run = runRepository.save(run);

        appendLog(run, "RECEIVED", "INFO",
                "SCMS 已收到 ESP32 原始上报，uploadSequence=" + telemetryEvent.getUploadSequence()
                        + "，telemetryEventId=" + telemetryEvent.getId(),
                telemetryEvent.getPayloadJson());
        appendLog(run, "PARSED", "INFO",
                "已解析家庭数字孪生：房门=" + cnOpen(snapshot.doorOpen())
                        + "，车库门=" + cnOpen(snapshot.garageOpen())
                        + "，风扇=" + cnOn(snapshot.fanOn())
                        + "，辅助灯=" + cnOn(snapshot.auxLedOn())
                        + "，主人在家=" + snapshot.ownerPresence(),
                run.getDecisionJson());
        appendLog(run, "ANALYZED", "INFO",
                "AI 自主巡检完成：场景=" + decision.sceneLabel() + "，风险=" + decision.riskLevel(),
                null);
        appendLog(run, "PERMISSION_CHECKED", "INFO",
                "权限校验结果：" + decision.permissionDecision() + "；" + decision.actionSummary(),
                run.getDecisionJson());
        appendLog(run, enrichAiReport ? "AI_REPORT_PENDING" : "AI_REPORT_SKIPPED", "INFO",
                enrichAiReport
                        ? "规则决策、权限校验和命令队列已先提交；DeepSeek 大模型巡检报告在后台补齐，避免阻塞 ESP32 执行动作。"
                        : "本轮为心跳、普通低风险或近期重复场景，保留规则引擎即时巡检报告，不再追加 DeepSeek 调用。",
                null);
        verifyClosedLoopCommands(run, telemetryEvent, payload);

        String lowRiskCommandType = decision.lowRiskCommandType();
        if (!lowRiskCommandType.isBlank()) {
            YinzhiAiDeviceCommandEntity command = buildCommand(run, lowRiskCommandType, "LOW", decision.actionSummary());
            commandRepository.save(command);
            appendLog(run, "COMMAND_QUEUED", "INFO",
                    "低风险命令已入队，等待 ESP32 轮询执行：commandType=" + lowRiskCommandType,
                    null);
            run.setStatus("COMMAND_QUEUED");
        }
        if (decision.blockedHighRisk()) {
            YinzhiAiDeviceCommandEntity blocked = buildCommand(run, "DOOR_GARAGE_CONTROL_BLOCKED", "HIGH", "门控属于高风险动作，AI 只记录拦截，不做云端自动控制。");
            blocked.setStatus("BLOCKED");
            commandRepository.save(blocked);
            appendLog(run, "BLOCKED", "WARNING", "高风险门控动作已被安全护栏拦截，需要人工确认。", null);
            run.setStatus(!lowRiskCommandType.isBlank() ? "COMMAND_QUEUED" : "BLOCKED");
        }
        runRepository.save(run);
        return run.getId();
    }

    private boolean shouldEnrichAiReport(Snapshot snapshot, Decision decision) {
        String triggerReason = normalize(snapshot.triggerReason());
        if (triggerReason.contains("HEARTBEAT") || "PARSE_ERROR".equals(triggerReason)) {
            return false;
        }
        return !"LOW".equals(normalize(decision.riskLevel()))
                || !decision.lowRiskCommandType().isBlank()
                || decision.blockedHighRisk();
    }

    private boolean hasRecentSimilarAiReport(Snapshot snapshot, Decision decision) {
        String deviceId = text(snapshot.deviceId(), "");
        if (deviceId.isEmpty()) {
            return false;
        }
        return runRepository.existsByDeviceIdAndTriggerReasonAndSceneLabelAndRiskLevelAndCreatedAtAfter(
                deviceId,
                snapshot.triggerReason(),
                decision.sceneLabel(),
                decision.riskLevel(),
                Instant.now().minus(AI_REPORT_DEDUP_WINDOW)
        );
    }

    private void enrichAiReport(Long runId, Snapshot snapshot, Decision decision) {
        AiResult aiResult = buildAiReport(snapshot, decision);
        transactionTemplate.executeWithoutResult(status -> runRepository.findById(runId).ifPresent(run -> {
            run.setAiProvider(aiResult.provider());
            run.setAiModel(aiResult.model());
            run.setAiFinishReason(aiResult.finishReason());
            run.setAiPromptTokens(aiResult.promptTokens());
            run.setAiCompletionTokens(aiResult.completionTokens());
            run.setAiTotalTokens(aiResult.totalTokens());
            run.setAiReport(aiResult.report());
            run.setFallbackReport(aiResult.fallback());
            runRepository.save(run);
            appendLog(run,
                    aiResult.fallback() ? "AI_REPORT_FALLBACK" : "AI_REPORT_READY",
                    aiResult.fallback() ? "WARNING" : "INFO",
                    aiResult.fallback()
                            ? "DeepSeek 大模型报告暂不可用，保留规则引擎即时巡检报告。"
                            : "DeepSeek 大模型巡检报告已后台补齐；命令队列未被模型响应时间阻塞。",
                    null);
        }));
    }

    private AiResult buildAiReport(Snapshot snapshot, Decision decision) {
        String fallback = buildFixedMarkdownReport(snapshot, decision);
        if (!aiCompletionService.isConfigured()) {
            return AiResult.fallback(fallback);
        }

        String userPrompt = buildFixedAiReportPrompt(snapshot, decision, fallback);

        try {
            AiCommitResponse response = aiCompletionService.complete(AI_REPORT_SYSTEM_PROMPT, userPrompt);
            AiCommitResponse.Usage usage = response.usage();
            String report = response.result();
            if (!isFixedTemplateReport(report)) {
                log.warn("Yinzhi AI inspection report ignored because it did not follow fixed template");
                return AiResult.fallback(fallback);
            }
            return new AiResult(
                    report,
                    false,
                    response.provider(),
                    response.model(),
                    response.finishReason(),
                    usage == null ? 0 : usage.promptTokens(),
                    usage == null ? 0 : usage.completionTokens(),
                    usage == null ? 0 : usage.totalTokens()
            );
        } catch (BusinessException exception) {
            log.warn("Yinzhi AI inspection completion unavailable, use fallback report: {}", exception.getMessage());
            return AiResult.fallback(fallback);
        } catch (RuntimeException exception) {
            log.warn("Yinzhi AI inspection completion failed, use fallback report", exception);
            return AiResult.fallback(fallback);
        }
    }

    private String buildFixedAiReportPrompt(Snapshot snapshot, Decision decision, String fixedTemplate) {
        return """
                请按下面《固定输出模板》生成巡检报告。
                硬性要求：
                - 最终回答只能输出模板正文，不要输出“好的”“以下是”等开场白。
                - 必须保留模板中的标题、字段名、字段顺序和模板版本号。
                - 必须尽量原样使用模板中的字段值，不要改写表达、不要改变事实、不要新增字段。
                - 闭环验证必须提到 ESP32 串口、服务端观察页、App 巡检观察台三端核对。

                《固定输出模板》
                %s

                《结构化输入，不要原样输出本段标题》
                - triggerReason: %s
                - uploadSequence: %s
                - telemetryEventId: %s
                - doorOpen: %s
                - garageOpen: %s
                - fanOn: %s
                - auxLedOn: %s
                - wifiConnected: %s
                - ownerPresence: %s
                - sceneLabel: %s
                - riskLevel: %s
                - permissionDecision: %s
                - actionSummary: %s
                """.formatted(
                fixedTemplate,
                snapshot.triggerReason(),
                snapshot.uploadSequence(),
                snapshot.telemetryEventId(),
                cnOpen(snapshot.doorOpen()),
                cnOpen(snapshot.garageOpen()),
                cnOn(snapshot.fanOn()),
                cnOn(snapshot.auxLedOn()),
                snapshot.wifiConnected() ? "在线" : "离线",
                ownerPresenceText(snapshot.ownerPresence()),
                decision.sceneLabel(),
                riskLevelText(decision.riskLevel()),
                decision.permissionDecision(),
                decision.actionSummary()
        );
    }

    private boolean isFixedTemplateReport(String report) {
        if (report == null || report.isBlank()) {
            return false;
        }
        return report.contains("### AI 自主巡检报告")
                && report.contains("模板版本：" + AI_REPORT_TEMPLATE_VERSION)
                && report.contains("- 触发原因：")
                && report.contains("- ESP32 上传序号：")
                && report.contains("- 遥测事件编号：")
                && report.contains("- 主人状态：")
                && report.contains("- 场景判断：")
                && report.contains("- 风险等级：")
                && report.contains("- 权限结论：")
                && report.contains("- 处置结果：")
                && report.contains("- 闭环验证：");
    }

    private Decision decide(Snapshot snapshot) {
        boolean anyDoorOpen = Boolean.TRUE.equals(snapshot.doorOpen()) || Boolean.TRUE.equals(snapshot.garageOpen());
        boolean ownerAway = "AWAY".equals(snapshot.ownerPresence());
        boolean visionUnknown = "UNKNOWN".equals(snapshot.ownerPresence());
        boolean auxHintNeeded = !Boolean.TRUE.equals(snapshot.auxLedOn());

        if (anyDoorOpen && ownerAway) {
            return new Decision(
                    "主人不在家且门控存在开启",
                    "HIGH",
                    "低风险提示可自动执行，高风险门控已拦截",
                    auxHintNeeded
                            ? "AI 将自动打开辅助灯提示并生成提醒，但不会从云端控制房门或车库门。"
                            : "辅助灯提示已处于开启状态，AI 不重复下发同类命令；门控高风险仍保持安全拦截。",
                    auxHintNeeded ? AUX_HINT_ON_COMMAND : "",
                    true
            );
        }
        if (anyDoorOpen && visionUnknown) {
            return new Decision(
                    "门控开启但主人状态未知",
                    "MEDIUM",
                    "先提示后确认",
                    auxHintNeeded
                            ? "摄像头状态不足，AI 只允许辅助灯提示和 App 提醒，门控动作保持人工确认。"
                            : "摄像头状态不足，辅助灯提示已开启，AI 不重复下发同类命令，门控动作保持人工确认。",
                    auxHintNeeded ? AUX_HINT_ON_COMMAND : "",
                    true
            );
        }
        if (!snapshot.wifiConnected()) {
            return new Decision(
                    "网关网络异常",
                    "MEDIUM",
                    "仅记录提醒",
                    "AI 记录网络异常并提示检查 WiFi，本地语音控制仍作为兜底。",
                    "",
                    false
            );
        }
        if (Boolean.TRUE.equals(snapshot.fanOn()) && ownerAway) {
            return new Decision(
                    "主人不在家但风扇仍开启",
                    "LOW",
                    "低风险设备可自动执行",
                    "AI 判断家中无人且风扇遗留开启，自动下发关闭风扇命令，并等待下一次遥测验证 fan_on=false。",
                    FAN_OFF_COMMAND,
                    false
            );
        }
        if (Boolean.TRUE.equals(snapshot.fanOn())) {
            return new Decision(
                    "通风运行正常",
                    "LOW",
                    "无需额外控制",
                    "风扇已处于开启状态，AI 只记录巡检结果并等待下一次状态回传。",
                    "",
                    false
            );
        }
        return new Decision(
                "家庭状态正常",
                "LOW",
                "无需额外控制",
                "门控、网络与视觉状态未触发风险，AI 完成本轮自主巡检并保留原始日志。",
                "",
                false
        );
    }

    private YinzhiAiDeviceCommandEntity buildCommand(YinzhiAiInspectionRunEntity run, String commandType, String riskLevel, String detail) {
        YinzhiAiDeviceCommandEntity command = new YinzhiAiDeviceCommandEntity();
        command.setRunId(run.getId());
        command.setTelemetryEventId(run.getTelemetryEventId());
        command.setDeviceId(run.getDeviceId());
        command.setUploadSequence(run.getUploadSequence());
        command.setCommandType(commandType);
        command.setRiskLevel(riskLevel);
        command.setStatus("QUEUED");
        command.setDetail(detail);
        return command;
    }

    private void verifyClosedLoopCommands(YinzhiAiInspectionRunEntity currentRun,
                                          IotDeviceTelemetryEventEntity telemetryEvent,
                                          JsonNode payload) {
        if (currentRun == null || telemetryEvent == null) {
            return;
        }
        String deviceId = text(telemetryEvent.getDeviceId(), "");
        Long uploadSequence = telemetryEvent.getUploadSequence();
        if (deviceId.isEmpty() || uploadSequence == null) {
            return;
        }

        List<YinzhiAiDeviceCommandEntity> candidates = commandRepository
                .findAllByDeviceIdAndStatusInAndUploadSequenceLessThanOrderByQueuedAtAscIdAsc(
                        deviceId,
                        CLOSED_LOOP_PENDING_STATUSES,
                        uploadSequence
                );
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        for (YinzhiAiDeviceCommandEntity command : candidates) {
            VerificationOutcome outcome = verifyCommandAgainstTelemetry(command, telemetryEvent, payload);
            if (!outcome.verified()) {
                continue;
            }

            command.setStatus("VERIFIED");
            command.setVerifiedAt(Instant.now());
            command.setVerificationTelemetryEventId(telemetryEvent.getId());
            command.setVerificationUploadSequence(uploadSequence);
            command.setVerificationMessage(outcome.message());
            commandRepository.save(command);

            appendLog(command, "CLOSED_LOOP_VERIFIED", "INFO", outcome.message(), telemetryEvent.getPayloadJson());

            if (currentRun.getId() == null || !currentRun.getId().equals(command.getRunId())) {
                appendLog(currentRun, "CLOSED_LOOP_VERIFIED", "INFO", outcome.message(), telemetryEvent.getPayloadJson());
            }

            runRepository.findById(command.getRunId()).ifPresent(run -> {
                run.setStatus("VERIFIED");
                runRepository.save(run);
            });
        }
    }

    private VerificationOutcome verifyCommandAgainstTelemetry(YinzhiAiDeviceCommandEntity command,
                                                              IotDeviceTelemetryEventEntity telemetryEvent,
                                                              JsonNode payload) {
        String commandType = normalize(command == null ? null : command.getCommandType());
        return switch (commandType) {
            case "AUX_HINT_ON" -> {
                if (Boolean.TRUE.equals(boolAt(payload, "atmega", "aux_led_on"))) {
                    yield new VerificationOutcome(true,
                            "闭环验证通过：新的遥测已确认辅助灯开启，AI 命令已被 328 执行并反映到回传状态。");
                }
                yield new VerificationOutcome(false, "辅助灯仍未变为开启状态，等待下一次遥测继续验证。");
            }
            case "AUX_HINT_OFF" -> {
                if (Boolean.FALSE.equals(boolAt(payload, "atmega", "aux_led_on"))) {
                    yield new VerificationOutcome(true,
                            "闭环验证通过：新的遥测已确认辅助灯关闭，AI 命令已被 328 执行并反映到回传状态。");
                }
                yield new VerificationOutcome(false, "辅助灯仍未关闭，等待下一次遥测继续验证。");
            }
            case "FAN_ON" -> {
                if (Boolean.TRUE.equals(boolAt(payload, "atmega", "fan_on"))) {
                    yield new VerificationOutcome(true,
                            "闭环验证通过：新的遥测已确认风扇开启，AI 命令已被 328 执行并反映到回传状态。");
                }
                yield new VerificationOutcome(false, "风扇仍未开启，等待下一次遥测继续验证。");
            }
            case "FAN_OFF" -> {
                if (Boolean.FALSE.equals(boolAt(payload, "atmega", "fan_on"))) {
                    yield new VerificationOutcome(true,
                            "闭环验证通过：新的遥测已确认风扇关闭，AI 命令已被 328 执行并反映到回传状态。");
                }
                yield new VerificationOutcome(false, "风扇仍未关闭，等待下一次遥测继续验证。");
            }
            case "DOOR_CLOSE" -> {
                if (Boolean.FALSE.equals(boolAt(payload, "atmega", "door_open"))) {
                    yield new VerificationOutcome(true,
                            "闭环验证通过：新的遥测已确认房门关闭，App 授权后的 AI 高风险命令已被 328 执行并反映到回传状态。");
                }
                yield new VerificationOutcome(false, "房门仍未关闭，等待下一次遥测继续验证。");
            }
            case "GARAGE_CLOSE" -> {
                if (Boolean.FALSE.equals(boolAt(payload, "atmega", "garage_open"))) {
                    yield new VerificationOutcome(true,
                            "闭环验证通过：新的遥测已确认车库门关闭，App 授权后的 AI 高风险命令已被 328 执行并反映到回传状态。");
                }
                yield new VerificationOutcome(false, "车库门仍未关闭，等待下一次遥测继续验证。");
            }
            default -> new VerificationOutcome(false, "当前命令类型不参与闭环验证。");
        };
    }

    private void appendLog(YinzhiAiInspectionRunEntity run, String stage, String level, String message, String rawJson) {
        YinzhiAiInspectionLogEntity logEntry = new YinzhiAiInspectionLogEntity();
        logEntry.setRunId(run.getId());
        logEntry.setTelemetryEventId(run.getTelemetryEventId());
        logEntry.setDeviceId(run.getDeviceId());
        logEntry.setUploadSequence(run.getUploadSequence());
        logEntry.setStage(stage);
        logEntry.setLevel(level);
        logEntry.setMessage(message);
        logEntry.setRawJson(rawJson);
        logRepository.save(logEntry);
    }

    private void appendLog(YinzhiAiDeviceCommandEntity command, String stage, String level, String message, String rawJson) {
        if (command == null) {
            return;
        }
        YinzhiAiInspectionLogEntity logEntry = new YinzhiAiInspectionLogEntity();
        logEntry.setRunId(command.getRunId());
        logEntry.setTelemetryEventId(command.getTelemetryEventId());
        logEntry.setDeviceId(command.getDeviceId());
        logEntry.setUploadSequence(command.getUploadSequence());
        logEntry.setStage(stage);
        logEntry.setLevel(level);
        logEntry.setMessage(message);
        logEntry.setRawJson(rawJson);
        logRepository.save(logEntry);
    }

    private ObjectNode toDecisionJson(Snapshot snapshot, Decision decision) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("inspection_engine", "HomeMind.YinzhiAiInspectionService");
        root.put("telemetry_event_id", snapshot.telemetryEventId());
        root.put("upload_sequence", snapshot.uploadSequence());
        root.put("owner_presence", snapshot.ownerPresence());
        root.put("scene_label", decision.sceneLabel());
        root.put("risk_level", decision.riskLevel());
        root.put("permission_decision", decision.permissionDecision());
        root.put("action_summary", decision.actionSummary());
        root.put("low_risk_command_type", decision.lowRiskCommandType());
        root.put("queue_aux_hint", AUX_HINT_ON_COMMAND.equals(decision.lowRiskCommandType()));
        root.put("blocked_high_risk", decision.blockedHighRisk());
        return root;
    }

    private String buildFixedMarkdownReport(Snapshot snapshot, Decision decision) {
        List<String> lines = new ArrayList<>();
        lines.add("### AI 自主巡检报告");
        lines.add("- 模板版本：" + AI_REPORT_TEMPLATE_VERSION);
        lines.add("- 触发原因：" + snapshot.triggerReason());
        lines.add("- ESP32 上传序号：" + snapshot.uploadSequence());
        lines.add("- 遥测事件编号：" + snapshot.telemetryEventId());
        lines.add("- 主人状态：" + ownerPresenceText(snapshot.ownerPresence()) + "，摄像头只用于判断主人是否在家。");
        lines.add("- 场景判断：" + decision.sceneLabel());
        lines.add("- 风险等级：" + riskLevelText(decision.riskLevel()));
        lines.add("- 权限结论：" + decision.permissionDecision());
        lines.add("- 处置结果：" + decision.actionSummary());
        lines.add("- 闭环验证：请在 ESP32 串口、服务端观察页和 App 巡检观察台核对 uploadSequence / telemetryEventId / inspectionRunId；原始遥测、AI 决策 JSON、命令 ACK、下一条遥测验证或安全拦截记录均已留存。");
        return String.join("\n", lines);
    }

    private JsonNode parseJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize yinzhi inspection json failed", exception);
        }
    }

    private static String cnOpen(Boolean value) {
        if (value == null) {
            return "未知";
        }
        return value ? "开启" : "关闭";
    }

    private static String cnOn(Boolean value) {
        if (value == null) {
            return "未知";
        }
        return value ? "开启" : "关闭";
    }

    private static String ownerPresenceText(String value) {
        return switch (text(value, "UNKNOWN")) {
            case "HOME" -> "HOME（主人在家）";
            case "AWAY" -> "AWAY（主人不在家）";
            default -> "UNKNOWN（摄像头未确认）";
        };
    }

    private static String riskLevelText(String value) {
        return switch (text(value, "LOW")) {
            case "HIGH" -> "HIGH（高风险）";
            case "MEDIUM" -> "MEDIUM（中风险）";
            default -> "LOW（低风险）";
        };
    }

    private record Snapshot(
            Long telemetryEventId,
            String deviceId,
            Long uploadSequence,
            String triggerReason,
            Boolean doorOpen,
            Boolean garageOpen,
            Boolean fanOn,
            Boolean auxLedOn,
            boolean wifiConnected,
            String ownerPresence
    ) {
        static Snapshot from(JsonNode payload, IotDeviceTelemetryEventEntity event) {
            Boolean visionOnline = boolAt(payload, "vision", "online");
            Boolean visionLearned = boolAt(payload, "vision", "learned");
            Boolean hasTarget = boolAt(payload, "vision", "has_target");
            String ownerPresence = "UNKNOWN";
            if (Boolean.TRUE.equals(visionOnline) && Boolean.TRUE.equals(visionLearned) && hasTarget != null) {
                ownerPresence = hasTarget ? "HOME" : "AWAY";
            }
            return new Snapshot(
                    event.getId(),
                    event.getDeviceId(),
                    event.getUploadSequence(),
                    text(event.getEventName(), "telemetry"),
                    boolAt(payload, "atmega", "door_open"),
                    boolAt(payload, "atmega", "garage_open"),
                    boolAt(payload, "atmega", "fan_on"),
                    boolAt(payload, "atmega", "aux_led_on"),
                    Boolean.TRUE.equals(boolAt(payload, "network", "wifi_connected")),
                    ownerPresence
            );
        }
    }

    private record Decision(
            String sceneLabel,
            String riskLevel,
            String permissionDecision,
            String actionSummary,
            String lowRiskCommandType,
            boolean blockedHighRisk
    ) {
        Decision {
            lowRiskCommandType = text(lowRiskCommandType, "");
        }
    }

    private record FastInspectionResult(Long runId, boolean enrichAiReport) {
    }

    private record AiResult(
            String report,
            boolean fallback,
            String provider,
            String model,
            String finishReason,
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {
        static AiResult fallback(String report) {
            return new AiResult(report, true, "rule-engine", "HomeMind-rules-v1", "fallback", 0, 0, 0);
        }
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

    private static String text(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private record VerificationOutcome(boolean verified, String message) {
    }
}
