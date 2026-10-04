package com.scms.core.clubappworkspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceGroupEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoProfileEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoStepEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectEntity;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDemoRuntimeRequest;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoRequest;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectDemoStepRequest;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceGroupRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoEventRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoProfileRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoRuntimeSnapshotRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectDemoStepRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectRepository;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceProjectDemoService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClubAppWorkspaceProjectDemoServiceTest {

    private ClubAppWorkspaceGroupRepository groupRepository;
    private ClubAppWorkspaceProjectRepository projectRepository;
    private ClubAppWorkspaceProjectDemoProfileRepository profileRepository;
    private ClubAppWorkspaceProjectDemoStepRepository stepRepository;
    private ClubAppWorkspaceProjectDemoRuntimeSnapshotRepository runtimeSnapshotRepository;
    private ClubAppWorkspaceProjectDemoEventRepository eventRepository;
    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private CurrentUserProvider currentUserProvider;
    private ClubAppWorkspaceProjectDemoService service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(ClubAppWorkspaceGroupRepository.class);
        projectRepository = mock(ClubAppWorkspaceProjectRepository.class);
        profileRepository = mock(ClubAppWorkspaceProjectDemoProfileRepository.class);
        stepRepository = mock(ClubAppWorkspaceProjectDemoStepRepository.class);
        runtimeSnapshotRepository = mock(ClubAppWorkspaceProjectDemoRuntimeSnapshotRepository.class);
        eventRepository = mock(ClubAppWorkspaceProjectDemoEventRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);

        service = new ClubAppWorkspaceProjectDemoService(
                groupRepository,
                projectRepository,
                profileRepository,
                stepRepository,
                runtimeSnapshotRepository,
                eventRepository,
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                currentUserProvider,
                new ObjectMapper()
        );

        when(clubRepository.findById(9L)).thenReturn(Optional.of(club(9L, "Xiuyuan Club")));
    }

    @Test
    void getVisibleProjectDemoShouldReturnConfiguredProfileForStudent() {
        ClubAppWorkspaceGroupEntity group = group(11L, 9L, true);
        ClubAppWorkspaceProjectEntity project = project(31L, 11L, "Cycling Guardian", "cycling-guardian", true);
        ClubAppWorkspaceProjectDemoProfileEntity profile = profile(51L, 31L);
        profile.setOverviewTitle("Cycling Guardian Demo");
        profile.setOverviewBody("Demo overview");
        profile.setBridgeMode("APP_HUB");
        profile.setMockSnapshotJson("{\"sessionId\":\"demo-session\"}");

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "cycling-guardian"))
                .thenReturn(Optional.of(project));
        when(profileRepository.findFirstByProjectIdAndEnabledTrue(31L)).thenReturn(Optional.of(profile));
        when(stepRepository.findAllByDemoProfileIdAndEnabledTrueOrderBySortOrderAscIdAsc(51L)).thenReturn(List.of());

        var response = service.getVisibleProjectDemo(9L, "cycling-guardian");

        assertEquals("Cycling Guardian Demo", response.overviewTitle());
        assertEquals("APP_HUB", response.bridgeMode());
        assertEquals("demo-session", response.mockSnapshot().get("sessionId").asText());
    }

    @Test
    void saveManagerProjectDemoShouldPersistProfileAndChildren() {
        ClubAppWorkspaceGroupEntity group = group(11L, 9L, true);
        ClubAppWorkspaceProjectEntity project = project(31L, 11L, "Cycling Guardian", "cycling-guardian", true);
        ClubAppWorkspaceProjectDemoProfileEntity savedProfile = profile(51L, 31L);

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(9L, 5L)).thenReturn(true);
        when(groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group));
        when(projectRepository.findFirstByGroupIdInAndProjectKey(List.of(11L), "cycling-guardian"))
                .thenReturn(Optional.of(project));
        when(profileRepository.findFirstByProjectId(31L)).thenReturn(Optional.empty(), Optional.of(savedProfile));
        when(profileRepository.save(any())).thenAnswer(invocation -> {
            ClubAppWorkspaceProjectDemoProfileEntity entity = invocation.getArgument(0);
            setField(ClubAppWorkspaceProjectDemoProfileEntity.class, entity, "id", 51L);
            savedProfile.setOverviewTitle(entity.getOverviewTitle());
            savedProfile.setOverviewBody(entity.getOverviewBody());
            savedProfile.setBridgeMode(entity.getBridgeMode());
            savedProfile.setEnabled(entity.getEnabled());
            savedProfile.setMockSnapshotJson(entity.getMockSnapshotJson());
            return entity;
        });
        when(stepRepository.findAllByDemoProfileIdOrderBySortOrderAscIdAsc(51L)).thenReturn(List.of(), List.of());
        when(stepRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ObjectNode mockSnapshot = JsonNodeFactory.instance.objectNode();
        mockSnapshot.put("sessionId", "demo");

        var response = service.saveManagerProjectDemo(
                9L,
                "cycling-guardian",
                new ManagerClubAppWorkspaceProjectDemoRequest(
                        "Cycling Guardian Demo",
                        "Overview",
                        "APP_HUB",
                        true,
                        List.of(new ManagerClubAppWorkspaceProjectDemoStepRequest(
                                null,
                                "Road Detection",
                                "Detect road risks",
                                "SCENE",
                                "HELMET",
                                null,
                                true
                        )),
                        mockSnapshot
                )
        );

        assertEquals("Cycling Guardian Demo", response.overviewTitle());
        verify(stepRepository).saveAll(any());
    }

    @Test
    void saveManagerProjectDemoShouldPreserveExistingBridgeModeAndMockSnapshotWhenRequestOmitsThem() {
        ClubAppWorkspaceGroupEntity group = group(11L, 9L, true);
        ClubAppWorkspaceProjectEntity project = project(31L, 11L, "Qingyu Huxin", "qingyu-huxin", true);
        ClubAppWorkspaceProjectDemoProfileEntity existingProfile = profile(51L, 31L);
        existingProfile.setOverviewTitle("Existing Demo");
        existingProfile.setOverviewBody("Existing overview");
        existingProfile.setBridgeMode("APP_HUB");
        existingProfile.setEnabled(true);
        existingProfile.setMockSnapshotJson("{\"sessionId\":\"existing-demo\"}");

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(9L, 5L)).thenReturn(true);
        when(groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group));
        when(projectRepository.findFirstByGroupIdInAndProjectKey(List.of(11L), "qingyu-huxin"))
                .thenReturn(Optional.of(project));
        when(profileRepository.findFirstByProjectId(31L)).thenReturn(Optional.of(existingProfile), Optional.of(existingProfile));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.findAllByDemoProfileIdOrderBySortOrderAscIdAsc(51L)).thenReturn(List.of(), List.of());
        when(stepRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.saveManagerProjectDemo(
                9L,
                "qingyu-huxin",
                new ManagerClubAppWorkspaceProjectDemoRequest(
                        "Qingyu Demo",
                        "Overview",
                        null,
                        true,
                        List.of(new ManagerClubAppWorkspaceProjectDemoStepRequest(
                                null,
                                "Local Questionnaire",
                                "Collect the student's current emotional expression",
                                "MANUAL",
                                "Raspberry Pi Side",
                                null,
                                true
                        )),
                        null
                )
        );

        assertEquals("APP_HUB", response.bridgeMode());
        assertEquals("existing-demo", response.mockSnapshot().get("sessionId").asText());
        verify(stepRepository).saveAll(argThat(steps -> {
            if (!(steps instanceof List<?> items) || items.isEmpty()) {
                return false;
            }
            Object first = items.get(0);
            return first instanceof ClubAppWorkspaceProjectDemoStepEntity entity
                    && "Raspberry Pi Side".equals(entity.getTargetSubsystem());
        }));
    }

    @Test
    void getVisibleProjectDemoRuntimeShouldFallbackToMockSnapshotWhenNoRuntimeSaved() {
        ClubAppWorkspaceGroupEntity group = group(11L, 9L, true);
        ClubAppWorkspaceProjectEntity project = project(31L, 11L, "Cycling Guardian", "cycling-guardian", true);
        ClubAppWorkspaceProjectDemoProfileEntity profile = profile(51L, 31L);
        profile.setMockSnapshotJson("""
                {
                  "sessionId":"demo-session",
                  "watch":{"online":false},
                  "helmet":{"riskLevel":"MEDIUM"},
                  "app":{"navigationHint":"slow down"},
                  "alerts":[{"title":"risk"}],
                  "recommendation":{"title":"be careful"}
                }
                """);

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "cycling-guardian"))
                .thenReturn(Optional.of(project));
        when(profileRepository.findFirstByProjectId(31L)).thenReturn(Optional.of(profile));
        when(runtimeSnapshotRepository.findFirstByProjectId(31L)).thenReturn(Optional.empty());
        when(eventRepository.findTop12ByProjectIdOrderByEventAtDescIdDesc(31L)).thenReturn(List.of());

        var response = service.getVisibleProjectDemoRuntime(9L, "cycling-guardian");

        assertEquals("demo-session", response.sessionId());
        assertEquals("MEDIUM", response.helmet().get("riskLevel").asText());
    }

    @Test
    void saveVisibleProjectDemoRuntimeShouldRejectUnauthorizedViewer() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(18L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 18L)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveVisibleProjectDemoRuntime(
                        9L,
                        "cycling-guardian",
                        new ClubAppWorkspaceProjectDemoRuntimeRequest(
                                "demo",
                                "APP",
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(runtimeSnapshotRepository, never()).save(any());
    }

    @Test
    void saveVisibleProjectDemoRuntimeShouldPersistPayload() {
        ClubAppWorkspaceGroupEntity group = group(11L, 9L, true);
        ClubAppWorkspaceProjectEntity project = project(31L, 11L, "Cycling Guardian", "cycling-guardian", true);

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(group));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "cycling-guardian"))
                .thenReturn(Optional.of(project));
        when(runtimeSnapshotRepository.findFirstByProjectId(31L))
                .thenReturn(Optional.empty(), Optional.of(runtimeSnapshot(71L, 31L, "{\"sessionId\":\"demo-session\"}")));
        when(runtimeSnapshotRepository.save(any())).thenAnswer(invocation -> {
            ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                setField(ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity.class, entity, "id", 71L);
            }
            return entity;
        });
        when(profileRepository.findFirstByProjectId(31L)).thenReturn(Optional.empty());
        when(eventRepository.findTop12ByProjectIdOrderByEventAtDescIdDesc(31L)).thenReturn(List.of());

        ObjectNode watch = JsonNodeFactory.instance.objectNode();
        watch.put("heartRate", 88);

        var response = service.saveVisibleProjectDemoRuntime(
                9L,
                "cycling-guardian",
                new ClubAppWorkspaceProjectDemoRuntimeRequest(
                        "demo-session",
                        "APP",
                        watch,
                        null,
                        null,
                        null,
                        null,
                        List.of()
                )
        );

        assertEquals("demo-session", response.sessionId());
        verify(runtimeSnapshotRepository).save(any());
    }

    private ClubEntity club(Long id, String name) {
        ClubEntity club = new ClubEntity();
        setField(ClubEntity.class, club, "id", id);
        club.setName(name);
        return club;
    }

    private ClubAppWorkspaceGroupEntity group(Long id, Long clubId, boolean enabled) {
        ClubAppWorkspaceGroupEntity group = new ClubAppWorkspaceGroupEntity();
        setField(ClubAppWorkspaceGroupEntity.class, group, "id", id);
        group.setClubId(clubId);
        group.setTitle("Innovation Contest");
        group.setSubtitle("");
        group.setEnabled(enabled);
        group.setSortOrder(1);
        group.setCreatedBy(5L);
        group.setUpdatedBy(5L);
        return group;
    }

    private ClubAppWorkspaceProjectEntity project(Long id, Long groupId, String title, String projectKey, boolean enabled) {
        ClubAppWorkspaceProjectEntity project = new ClubAppWorkspaceProjectEntity();
        setField(ClubAppWorkspaceProjectEntity.class, project, "id", id);
        project.setGroupId(groupId);
        project.setProjectKey(projectKey);
        project.setTitle(title);
        project.setSubtitle("AI safety demo");
        project.setSummary("Project summary");
        project.setEnabled(enabled);
        project.setSortOrder(1);
        project.setCreatedBy(5L);
        project.setUpdatedBy(5L);
        return project;
    }

    private ClubAppWorkspaceProjectDemoProfileEntity profile(Long id, Long projectId) {
        ClubAppWorkspaceProjectDemoProfileEntity profile = new ClubAppWorkspaceProjectDemoProfileEntity();
        setField(ClubAppWorkspaceProjectDemoProfileEntity.class, profile, "id", id);
        profile.setProjectId(projectId);
        profile.setEnabled(true);
        profile.setCreatedBy(5L);
        profile.setUpdatedBy(5L);
        return profile;
    }

    private ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity runtimeSnapshot(Long id, Long projectId, String payloadJson) {
        ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity snapshot = new ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity();
        setField(ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity.class, snapshot, "id", id);
        snapshot.setProjectId(projectId);
        snapshot.setSourceType("APP");
        snapshot.setSourceSessionId("demo-session");
        snapshot.setPayloadJson(payloadJson);
        snapshot.setCreatedBy(5L);
        return snapshot;
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
