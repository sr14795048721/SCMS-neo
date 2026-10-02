package com.scms.core.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.scms.core.iot.repository.IotDeviceTelemetryEventRepository;
import com.scms.core.iot.repository.IotDeviceTelemetryLatestStateRepository;
import com.scms.core.iot.service.IotProjectTelemetryQueryService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IotProjectTelemetryQueryServiceTest {

    private IotDeviceTelemetryLatestStateRepository latestStateRepository;
    private IotDeviceTelemetryEventRepository eventRepository;
    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ClubAppWorkspaceGroupRepository groupRepository;
    private ClubAppWorkspaceProjectRepository projectRepository;
    private CurrentUserProvider currentUserProvider;
    private IotProjectTelemetryQueryService service;

    @BeforeEach
    void setUp() {
        latestStateRepository = mock(IotDeviceTelemetryLatestStateRepository.class);
        eventRepository = mock(IotDeviceTelemetryEventRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        groupRepository = mock(ClubAppWorkspaceGroupRepository.class);
        projectRepository = mock(ClubAppWorkspaceProjectRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);

        service = new IotProjectTelemetryQueryService(
                latestStateRepository,
                eventRepository,
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                groupRepository,
                projectRepository,
                currentUserProvider,
                new ObjectMapper()
        );

        when(clubRepository.findById(9L)).thenReturn(Optional.of(club(9L, "Xiuyuan Club")));
    }

    @Test
    void getVisibleProjectTelemetryShouldReturnAggregatedResponseForStudent() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group(11L, 9L)));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "yinzhi-guanjia"))
                .thenReturn(Optional.of(project(31L, 11L, "隐智管家", "yinzhi-guanjia")));
        when(latestStateRepository.findAllByProjectOrderByLastReceivedAtDescDeviceIdAsc("yinzhi_guanjia"))
                .thenReturn(List.of(latestState("esp32s3_test", Instant.parse("2026-04-23T10:00:00Z"))));
        when(eventRepository.findAllByProjectOrderByReceivedAtDescIdDesc("yinzhi_guanjia", PageRequest.of(0, 24)))
                .thenReturn(new PageImpl<>(
                        List.of(event(81L, "vision_result", Instant.parse("2026-04-23T10:01:00Z"))),
                        PageRequest.of(0, 24),
                        31
                ));

        var response = service.getVisibleProjectTelemetry(9L, "yinzhi-guanjia");

        assertEquals("yinzhi-guanjia", response.projectKey());
        assertEquals("yinzhi_guanjia", response.telemetryProjectKey());
        assertEquals("ESP32_GATEWAY", response.integrationMode());
        assertTrue(response.hasTelemetry());
        assertEquals(1, response.devices().size());
        assertEquals("esp32s3_test", response.devices().getFirst().deviceId());
        assertEquals("HIGH", response.devices().getFirst().edgeInference().path("overall_risk").asText());
        assertEquals("vision_result", response.recentEvents().getFirst().event());
        assertEquals("HIGH", response.recentEvents().getFirst().edgeInference().path("overall_risk").asText());
        assertEquals("HIGH", response.recentEvents().getFirst().payload()
                .path("edge_inference")
                .path("overall_risk")
                .asText());
        assertEquals(0, response.eventPage());
        assertEquals(24, response.eventSize());
        assertEquals(31, response.eventTotal());
        assertEquals(true, response.hasMoreEvents());
    }

    @Test
    void getVisibleProjectTelemetryShouldClampEventPagination() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group(11L, 9L)));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "yinzhi-guanjia"))
                .thenReturn(Optional.of(project(31L, 11L, "隐智管家", "yinzhi-guanjia")));
        when(latestStateRepository.findAllByProjectOrderByLastReceivedAtDescDeviceIdAsc("yinzhi_guanjia"))
                .thenReturn(List.of());
        when(eventRepository.findAllByProjectOrderByReceivedAtDescIdDesc("yinzhi_guanjia", PageRequest.of(0, 60)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 60), 0));

        var response = service.getVisibleProjectTelemetry(9L, "yinzhi-guanjia", -2, 999);

        assertEquals(0, response.eventPage());
        assertEquals(60, response.eventSize());
    }

    @Test
    void getVisibleProjectTelemetryShouldReturnEmptyStateWhenNoTelemetry() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group(11L, 9L)));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "yinzhi-guanjia"))
                .thenReturn(Optional.of(project(31L, 11L, "隐智管家", "yinzhi-guanjia")));
        when(latestStateRepository.findAllByProjectOrderByLastReceivedAtDescDeviceIdAsc("yinzhi_guanjia"))
                .thenReturn(List.of());
        when(eventRepository.findAllByProjectOrderByReceivedAtDescIdDesc("yinzhi_guanjia", PageRequest.of(0, 24)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 24), 0));

        var response = service.getVisibleProjectTelemetry(9L, "yinzhi-guanjia");

        assertEquals("ESP32_GATEWAY", response.integrationMode());
        assertEquals(false, response.hasTelemetry());
        assertEquals(0, response.devices().size());
        assertEquals(0, response.recentEvents().size());
    }

    @Test
    void getVisibleProjectTelemetryShouldFallbackWhenPayloadMalformed() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group(11L, 9L)));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "yinzhi-guanjia"))
                .thenReturn(Optional.of(project(31L, 11L, "隐智管家", "yinzhi-guanjia")));

        IotDeviceTelemetryLatestStateEntity entity = latestState("esp32s3_test", Instant.parse("2026-04-23T10:00:00Z"));
        entity.setPayloadJson("{broken");
        when(latestStateRepository.findAllByProjectOrderByLastReceivedAtDescDeviceIdAsc("yinzhi_guanjia"))
                .thenReturn(List.of(entity));
        IotDeviceTelemetryEventEntity event = event(82L, "broken_event", Instant.parse("2026-04-23T10:02:00Z"));
        event.setPayloadJson("{broken");
        when(eventRepository.findAllByProjectOrderByReceivedAtDescIdDesc("yinzhi_guanjia", PageRequest.of(0, 24)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 24), 1));

        var response = service.getVisibleProjectTelemetry(9L, "yinzhi-guanjia");

        assertTrue(response.devices().getFirst().payload().isObject());
        assertEquals(0, response.devices().getFirst().payload().size());
        assertTrue(response.devices().getFirst().edgeInference().isObject());
        assertEquals(0, response.devices().getFirst().edgeInference().size());
        assertTrue(response.recentEvents().getFirst().payload().isObject());
        assertEquals(0, response.recentEvents().getFirst().payload().size());
        assertTrue(response.recentEvents().getFirst().edgeInference().isObject());
        assertEquals(0, response.recentEvents().getFirst().edgeInference().size());
    }

    @Test
    void getVisibleProjectTelemetryShouldRejectUnauthorizedViewer() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(18L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 18L)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getVisibleProjectTelemetry(9L, "yinzhi-guanjia")
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(groupRepository, never()).findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L);
    }

    @Test
    void getVisibleProjectTelemetryShouldRejectInvisibleProject() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group(11L, 9L)));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "yinzhi-guanjia"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getVisibleProjectTelemetry(9L, "yinzhi-guanjia")
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(latestStateRepository, never()).findAllByProjectOrderByLastReceivedAtDescDeviceIdAsc("yinzhi_guanjia");
    }

    private ClubEntity club(Long id, String name) {
        ClubEntity club = new ClubEntity();
        setField(ClubEntity.class, club, "id", id);
        club.setName(name);
        return club;
    }

    private ClubAppWorkspaceGroupEntity group(Long id, Long clubId) {
        ClubAppWorkspaceGroupEntity group = new ClubAppWorkspaceGroupEntity();
        setField(ClubAppWorkspaceGroupEntity.class, group, "id", id);
        group.setClubId(clubId);
        group.setTitle("Innovation Contest");
        group.setEnabled(true);
        group.setCreatedBy(5L);
        group.setUpdatedBy(5L);
        return group;
    }

    private ClubAppWorkspaceProjectEntity project(Long id, Long groupId, String title, String projectKey) {
        ClubAppWorkspaceProjectEntity project = new ClubAppWorkspaceProjectEntity();
        setField(ClubAppWorkspaceProjectEntity.class, project, "id", id);
        project.setGroupId(groupId);
        project.setProjectKey(projectKey);
        project.setTitle(title);
        project.setSubtitle("智能网关演示");
        project.setSummary("summary");
        project.setEnabled(true);
        project.setCreatedBy(5L);
        project.setUpdatedBy(5L);
        return project;
    }

    private IotDeviceTelemetryLatestStateEntity latestState(String deviceId, Instant lastReceivedAt) {
        IotDeviceTelemetryLatestStateEntity entity = new IotDeviceTelemetryLatestStateEntity();
        entity.setDeviceId(deviceId);
        entity.setProject("yinzhi_guanjia");
        entity.setGatewayType("esp32s3_gateway");
        entity.setFirmware("esp32_state_receiver_net_v1");
        entity.setMac("28:37:2f:89:82:4c");
        entity.setEventName("heartbeat");
        entity.setUploadSequence(6L);
        entity.setSentAtMs(84512L);
        entity.setGatewayUptimeMs(84512L);
        entity.setPendingReason("heartbeat");
        entity.setWifiConnected(true);
        entity.setIp("192.168.1.88");
        entity.setRssi(-48);
        entity.setPayloadJson("""
                {
                  "device": {
                    "device_id": "esp32s3_test"
                  },
                  "edge_inference": {
                    "status": "ok",
                    "overall_risk": "HIGH",
                    "action_code": "WATER_RETENTION",
                    "crop_group": "ACID_DRY_TOLERANT"
                  }
                }
                """);
        entity.setFirstReceivedAt(lastReceivedAt.minusSeconds(10));
        entity.setLastReceivedAt(lastReceivedAt);
        return entity;
    }

    private IotDeviceTelemetryEventEntity event(Long id, String eventName, Instant receivedAt) {
        IotDeviceTelemetryEventEntity entity = new IotDeviceTelemetryEventEntity();
        setField(IotDeviceTelemetryEventEntity.class, entity, "id", id);
        entity.setProject("yinzhi_guanjia");
        entity.setDeviceId("esp32s3_test");
        entity.setGatewayType("esp32s3_gateway");
        entity.setEventName(eventName);
        entity.setUploadSequence(6L);
        entity.setSentAtMs(84512L);
        entity.setPendingReason("vision_result");
        entity.setWifiConnected(true);
        entity.setIp("192.168.1.88");
        entity.setRssi(-48);
        entity.setPayloadJson("""
                {
                  "edge_inference": {
                    "status": "ok",
                    "overall_risk": "HIGH",
                    "action_code": "WATER_RETENTION",
                    "crop_group": "ACID_DRY_TOLERANT"
                  }
                }
                """);
        setField(IotDeviceTelemetryEventEntity.class, entity, "receivedAt", receivedAt);
        return entity;
    }

    private void setField(Class<?> type, Object target, String name, Object value) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
