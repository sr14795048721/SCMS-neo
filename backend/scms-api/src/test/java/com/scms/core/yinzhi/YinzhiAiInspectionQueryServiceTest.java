package com.scms.core.yinzhi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceGroupEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectEntity;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceGroupRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import com.scms.core.yinzhi.domain.YinzhiAiDeviceCommandEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionLogEntity;
import com.scms.core.yinzhi.domain.YinzhiAiInspectionRunEntity;
import com.scms.core.yinzhi.dto.YinzhiManualConfirmRequest;
import com.scms.core.yinzhi.repository.YinzhiAiDeviceCommandRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionLogRepository;
import com.scms.core.yinzhi.repository.YinzhiAiInspectionRunRepository;
import com.scms.core.yinzhi.service.YinzhiAiInspectionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YinzhiAiInspectionQueryServiceTest {

    private YinzhiAiInspectionRunRepository runRepository;
    private YinzhiAiInspectionLogRepository logRepository;
    private YinzhiAiDeviceCommandRepository commandRepository;
    private ClubRepository clubRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ClubAppWorkspaceGroupRepository groupRepository;
    private ClubAppWorkspaceProjectRepository projectRepository;
    private CurrentUserProvider currentUserProvider;
    private YinzhiAiInspectionQueryService service;

    @BeforeEach
    void setUp() {
        runRepository = mock(YinzhiAiInspectionRunRepository.class);
        logRepository = mock(YinzhiAiInspectionLogRepository.class);
        commandRepository = mock(YinzhiAiDeviceCommandRepository.class);
        clubRepository = mock(ClubRepository.class);
        ClubManagerBindingRepository clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        groupRepository = mock(ClubAppWorkspaceGroupRepository.class);
        projectRepository = mock(ClubAppWorkspaceProjectRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);

        service = new YinzhiAiInspectionQueryService(
                runRepository,
                logRepository,
                commandRepository,
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                groupRepository,
                projectRepository,
                currentUserProvider,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void confirmHighRiskManualHandlingShouldRecordAppConfirmationAndQueueCloseCommands() {
        YinzhiAiInspectionRunEntity run = sampleRun();
        YinzhiAiDeviceCommandEntity command = sampleBlockedCommand();
        ClubAppWorkspaceGroupEntity group = mock(ClubAppWorkspaceGroupEntity.class);

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(7L, "student-a", UserRole.STUDENT));
        when(clubRepository.findById(1L)).thenReturn(Optional.of(mock(ClubEntity.class)));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(1L, 7L)).thenReturn(true);
        when(group.getId()).thenReturn(2L);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(group));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(any(), eq("yinzhi-guanjia")))
                .thenReturn(Optional.of(mock(ClubAppWorkspaceProjectEntity.class)));
        when(commandRepository.findById(12L)).thenReturn(Optional.of(command));
        when(runRepository.findById(55L)).thenReturn(Optional.of(run));
        when(runRepository.findFirstByOrderByCreatedAtDescIdDesc()).thenReturn(Optional.of(run));
        when(logRepository.findAllByRunIdOrderByCreatedAtAscIdAsc(run.getId())).thenReturn(List.of());
        when(commandRepository.findAllByRunIdOrderByQueuedAtAscIdAsc(run.getId())).thenReturn(List.of(command));
        when(commandRepository.findAllByVerificationTelemetryEventIdOrderByQueuedAtAscIdAsc(101L)).thenReturn(List.of());
        when(runRepository.findTop10ByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(run));

        service.confirmHighRiskManualHandling(
                1L,
                "yinzhi-guanjia",
                12L,
                new YinzhiManualConfirmRequest("现场已确认")
        );

        assertEquals("MANUAL_CONFIRMED", command.getStatus());
        assertEquals("COMMAND_QUEUED", run.getStatus());
        assertTrue(command.getAckMessage().contains("student-a"));
        assertTrue(command.getAckMessage().contains("现场已确认"));
        assertTrue(command.getAckMessage().contains("授权自动执行关闭动作"));

        ArgumentCaptor<YinzhiAiDeviceCommandEntity> commandCaptor =
                ArgumentCaptor.forClass(YinzhiAiDeviceCommandEntity.class);
        verify(commandRepository, times(3)).save(commandCaptor.capture());
        List<String> commandTypes = commandCaptor.getAllValues().stream()
                .map(YinzhiAiDeviceCommandEntity::getCommandType)
                .toList();
        assertTrue(commandTypes.contains("DOOR_CLOSE"));
        assertTrue(commandTypes.contains("GARAGE_CLOSE"));

        ArgumentCaptor<YinzhiAiInspectionLogEntity> logCaptor =
                ArgumentCaptor.forClass(YinzhiAiInspectionLogEntity.class);
        verify(logRepository, atLeast(3)).save(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream()
                .anyMatch(log -> "MANUAL_CONFIRMED".equals(log.getStage())
                        && log.getMessage().contains("授权自动执行关闭动作")));
        assertTrue(logCaptor.getAllValues().stream()
                .anyMatch(log -> "AUTHORIZED_COMMAND_QUEUED".equals(log.getStage())
                        && log.getMessage().contains("DOOR_CLOSE")));
        assertTrue(logCaptor.getAllValues().stream()
                .anyMatch(log -> "AUTHORIZED_COMMAND_QUEUED".equals(log.getStage())
                        && log.getMessage().contains("GARAGE_CLOSE")));
    }

    private YinzhiAiInspectionRunEntity sampleRun() {
        YinzhiAiInspectionRunEntity run = new YinzhiAiInspectionRunEntity();
        run.setTelemetryEventId(101L);
        run.setDeviceId("esp32s3_B43A45A60D48");
        run.setUploadSequence(9L);
        run.setTriggerReason("atmega_state_update");
        run.setOwnerPresence("AWAY");
        run.setSceneLabel("主人不在家且门控状态异常");
        run.setRiskLevel("HIGH");
        run.setPermissionDecision("高风险动作必须人工确认");
        run.setActionSummary("门控动作已被安全护栏拦截");
        run.setAiProvider("fallback");
        run.setAiModel("rule-engine");
        run.setAiFinishReason("fallback");
        run.setAiReport("### AI 自主巡检报告");
        run.setFallbackReport(true);
        run.setRawTelemetryJson("""
                {
                  "atmega": {
                    "door_open": true,
                    "garage_open": true
                  }
                }
                """);
        run.setDecisionJson("{}");
        run.setStatus("BLOCKED");
        return run;
    }

    private YinzhiAiDeviceCommandEntity sampleBlockedCommand() {
        YinzhiAiDeviceCommandEntity command = new YinzhiAiDeviceCommandEntity();
        command.setRunId(55L);
        command.setTelemetryEventId(101L);
        command.setDeviceId("esp32s3_B43A45A60D48");
        command.setUploadSequence(9L);
        command.setCommandType("DOOR_GARAGE_CONTROL_BLOCKED");
        command.setRiskLevel("HIGH");
        command.setStatus("BLOCKED");
        command.setDetail("门控属于高风险动作，AI 只记录拦截。");
        return command;
    }
}
