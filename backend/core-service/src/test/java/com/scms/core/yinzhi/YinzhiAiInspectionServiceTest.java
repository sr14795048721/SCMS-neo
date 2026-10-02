package com.scms.core.yinzhi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.ai.dto.AiCommitResponse;
import com.scms.core.ai.service.ScmsAiCompletionService;
import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import com.scms.core.yinzhi.domain.YinzhiAiDeviceCommandEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionLogEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionRunEntity;
import com.scms.core.yinzhi.repository.YinzhiAiDeviceCommandRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionLogRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionRunRepository;
import com.scms.core.yinzhi.service.YinzhiAiInspectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YinzhiAiInspectionServiceTest {

    private YinzhiAiInspectionRunRepository runRepository;
    private YinzhiAiInspectionLogRepository logRepository;
    private YinzhiAiDeviceCommandRepository commandRepository;
    private ScmsAiCompletionService aiCompletionService;
    private YinzhiAiInspectionService service;

    @BeforeEach
    void setUp() {
        runRepository = mock(YinzhiAiInspectionRunRepository.class);
        logRepository = mock(YinzhiAiInspectionLogRepository.class);
        commandRepository = mock(YinzhiAiDeviceCommandRepository.class);
        aiCompletionService = mock(ScmsAiCompletionService.class);
        service = new YinzhiAiInspectionService(
                runRepository,
                logRepository,
                commandRepository,
                aiCompletionService,
                new ObjectMapper().findAndRegisterModules(),
                transactionTemplate()
        );

        when(runRepository.save(any(YinzhiAiInspectionRunEntity.class)))
                .thenAnswer(invocation -> {
                    YinzhiAiInspectionRunEntity run = invocation.getArgument(0);
                    if (run.getId() == null) {
                        setRunId(run, 501L);
                    }
                    return run;
                });
    }

    @Test
    void inspectTelemetryEventShouldUseFixedAiReportPromptTemplate() throws Exception {
        when(runRepository.existsByTelemetryEventId(99L)).thenReturn(false);
        when(aiCompletionService.isConfigured()).thenReturn(true);
        when(aiCompletionService.complete(any(), any())).thenReturn(new AiCommitResponse(
                """
                        ### AI 自主巡检报告
                        - 模板版本：YINZHI_AI_INSPECTION_REPORT_V1
                        - 触发原因：atmega_state_update
                        - ESP32 上传序号：8
                        - 遥测事件编号：99
                        - 主人状态：HOME（主人在家）
                        - 场景判断：家庭状态正常
                        - 风险等级：LOW（低风险）
                        - 权限结论：无需额外控制
                        - 处置结果：完成巡检
                        - 闭环验证：请在 ESP32 串口、服务端观察页和 App 巡检观察台核对。
                        """.trim(),
                "siliconflow",
                "test-model",
                "stop",
                new AiCommitResponse.Usage(10, 20, 30)
        ));

        service.inspectTelemetryEvent(sampleTelemetryEvent());

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiCompletionService).complete(systemPromptCaptor.capture(), userPromptCaptor.capture());

        String systemPrompt = systemPromptCaptor.getValue();
        String userPrompt = userPromptCaptor.getValue();
        assertTrue(systemPrompt.contains("固定模板"));
        assertTrue(systemPrompt.contains("禁止更换标题、字段名和字段顺序"));
        assertTrue(userPrompt.contains("### AI 自主巡检报告"));
        assertTrue(userPrompt.contains("模板版本：YINZHI_AI_INSPECTION_REPORT_V1"));
        assertTrue(userPrompt.contains("- ESP32 上传序号：8"));
        assertTrue(userPrompt.contains("- 遥测事件编号：99"));
        assertTrue(userPrompt.contains("- 主人状态：AWAY（主人不在家）"));
        assertTrue(userPrompt.contains("- 闭环验证："));
    }

    @Test
    void inspectTelemetryEventShouldSkipAiReportForHeartbeat() throws Exception {
        when(runRepository.existsByTelemetryEventId(101L)).thenReturn(false);
        when(aiCompletionService.isConfigured()).thenReturn(true);

        service.inspectTelemetryEvent(sampleHeartbeatTelemetryEvent());

        verify(aiCompletionService, never()).complete(any(), any());

        ArgumentCaptor<YinzhiAiInspectionLogEntity> logCaptor = ArgumentCaptor.forClass(YinzhiAiInspectionLogEntity.class);
        verify(logRepository, org.mockito.Mockito.atLeastOnce()).save(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream()
                .anyMatch(log -> "AI_REPORT_SKIPPED".equals(log.getStage())
                        && log.getMessage().contains("不再追加 DeepSeek 调用")));
    }

    @Test
    void inspectTelemetryEventShouldDeduplicateRecentSimilarAiReport() throws Exception {
        when(runRepository.existsByTelemetryEventId(99L)).thenReturn(false);
        when(runRepository.existsByDeviceIdAndTriggerReasonAndSceneLabelAndRiskLevelAndCreatedAtAfter(
                any(), any(), any(), any(), any()
        )).thenReturn(true);

        service.inspectTelemetryEvent(sampleTelemetryEvent());

        verify(aiCompletionService, never()).complete(any(), any());
        verify(runRepository, org.mockito.Mockito.never()).findById(any());
    }

    @Test
    void inspectTelemetryEventShouldQueueFanOffWhenOwnerAwayAndFanStillOn() throws Exception {
        when(runRepository.existsByTelemetryEventId(102L)).thenReturn(false);
        when(aiCompletionService.isConfigured()).thenReturn(false);

        service.inspectTelemetryEvent(sampleOwnerAwayFanOnTelemetryEvent());

        ArgumentCaptor<YinzhiAiDeviceCommandEntity> commandCaptor =
                ArgumentCaptor.forClass(YinzhiAiDeviceCommandEntity.class);
        verify(commandRepository).save(commandCaptor.capture());
        YinzhiAiDeviceCommandEntity command = commandCaptor.getValue();
        assertEquals("FAN_OFF", command.getCommandType());
        assertEquals("LOW", command.getRiskLevel());
        assertEquals("QUEUED", command.getStatus());
        assertTrue(command.getDetail().contains("自动下发关闭风扇命令"));

        ArgumentCaptor<YinzhiAiInspectionLogEntity> logCaptor = ArgumentCaptor.forClass(YinzhiAiInspectionLogEntity.class);
        verify(logRepository, org.mockito.Mockito.atLeastOnce()).save(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream()
                .anyMatch(log -> "COMMAND_QUEUED".equals(log.getStage())
                        && log.getMessage().contains("commandType=FAN_OFF")));
    }

    @Test
    void inspectTelemetryEventShouldVerifyClosedLoopCommandFromNextTelemetry() throws Exception {
        when(runRepository.existsByTelemetryEventId(100L)).thenReturn(false);
        when(aiCompletionService.isConfigured()).thenReturn(false);

        YinzhiAiDeviceCommandEntity command = new YinzhiAiDeviceCommandEntity();
        command.setRunId(321L);
        command.setTelemetryEventId(99L);
        command.setDeviceId("esp32s3_B43A45A60D48");
        command.setUploadSequence(8L);
        command.setCommandType("AUX_HINT_ON");
        command.setRiskLevel("LOW");
        command.setStatus("ACKED");
        command.setDetail("AI 自动打开辅助灯提示");
        when(commandRepository.findAllByDeviceIdAndStatusInAndUploadSequenceLessThanOrderByQueuedAtAscIdAsc(
                "esp32s3_B43A45A60D48",
                List.of("DELIVERED", "ACKED"),
                9L
        )).thenReturn(List.of(command));

        YinzhiAiInspectionRunEntity originalRun = new YinzhiAiInspectionRunEntity();
        originalRun.setStatus("ACKED");
        when(runRepository.findById(321L)).thenReturn(Optional.of(originalRun));

        service.inspectTelemetryEvent(sampleVerifiedAuxTelemetryEvent());

        assertEquals("VERIFIED", command.getStatus());
        assertEquals(100L, command.getVerificationTelemetryEventId());
        assertEquals(9L, command.getVerificationUploadSequence());
        assertTrue(command.getVerificationMessage().contains("闭环验证通过"));
        assertEquals("VERIFIED", originalRun.getStatus());

        ArgumentCaptor<YinzhiAiInspectionLogEntity> logCaptor = ArgumentCaptor.forClass(YinzhiAiInspectionLogEntity.class);
        verify(logRepository, org.mockito.Mockito.atLeastOnce()).save(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream()
                .anyMatch(log -> "CLOSED_LOOP_VERIFIED".equals(log.getStage())
                        && log.getMessage().contains("辅助灯开启")));
    }

    private IotDeviceTelemetryEventEntity sampleTelemetryEvent() throws Exception {
        IotDeviceTelemetryEventEntity event = new IotDeviceTelemetryEventEntity();
        setId(event, 99L);
        event.setProject("yinzhi_guanjia");
        event.setDeviceId("esp32s3_B43A45A60D48");
        event.setUploadSequence(8L);
        event.setEventName("atmega_state_update");
        event.setPayloadJson("""
                {
                  "network": {"wifi_connected": true},
                  "atmega": {
                    "door_open": true,
                    "garage_open": false,
                    "fan_on": false,
                    "aux_led_on": false
                  },
                  "vision": {
                    "online": true,
                    "learned": true,
                    "has_target": false
                  }
                }
                """);
        return event;
    }

    private IotDeviceTelemetryEventEntity sampleHeartbeatTelemetryEvent() throws Exception {
        IotDeviceTelemetryEventEntity event = new IotDeviceTelemetryEventEntity();
        setId(event, 101L);
        event.setProject("yinzhi_guanjia");
        event.setDeviceId("esp32s3_B43A45A60D48");
        event.setUploadSequence(10L);
        event.setEventName("heartbeat");
        event.setPayloadJson("""
                {
                  "network": {"wifi_connected": true},
                  "atmega": {
                    "door_open": false,
                    "garage_open": false,
                    "fan_on": false,
                    "aux_led_on": false
                  },
                  "vision": {
                    "online": true,
                    "learned": true,
                    "has_target": true
                  }
                }
                """);
        return event;
    }

    private IotDeviceTelemetryEventEntity sampleOwnerAwayFanOnTelemetryEvent() throws Exception {
        IotDeviceTelemetryEventEntity event = new IotDeviceTelemetryEventEntity();
        setId(event, 102L);
        event.setProject("yinzhi_guanjia");
        event.setDeviceId("esp32s3_B43A45A60D48");
        event.setUploadSequence(11L);
        event.setEventName("atmega_state_update");
        event.setPayloadJson("""
                {
                  "network": {"wifi_connected": true},
                  "atmega": {
                    "door_open": false,
                    "garage_open": false,
                    "fan_on": true,
                    "aux_led_on": false
                  },
                  "vision": {
                    "online": true,
                    "learned": true,
                    "has_target": false
                  }
                }
                """);
        return event;
    }

    private IotDeviceTelemetryEventEntity sampleVerifiedAuxTelemetryEvent() throws Exception {
        IotDeviceTelemetryEventEntity event = new IotDeviceTelemetryEventEntity();
        setId(event, 100L);
        event.setProject("yinzhi_guanjia");
        event.setDeviceId("esp32s3_B43A45A60D48");
        event.setUploadSequence(9L);
        event.setEventName("atmega_state_update");
        event.setPayloadJson("""
                {
                  "network": {"wifi_connected": true},
                  "atmega": {
                    "door_open": false,
                    "garage_open": false,
                    "fan_on": false,
                    "aux_led_on": true,
                    "last_command": "ai_aux_on"
                  },
                  "vision": {
                    "online": true,
                    "learned": true,
                    "has_target": true
                  }
                }
                """);
        return event;
    }

    private void setId(IotDeviceTelemetryEventEntity event, Long id) throws Exception {
        Field field = IotDeviceTelemetryEventEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(event, id);
    }

    private void setRunId(YinzhiAiInspectionRunEntity run, Long id) throws Exception {
        Field field = YinzhiAiInspectionRunEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(run, id);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }
}
