package com.scms.core.clubappworkspace;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceGroupEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectMaterialEntity;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceGroupRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectMaterialRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceRequest;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceGroupRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectMaterialRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectRepository;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceMaterialContent;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceObjectStorageService;
import com.scms.core.clubappworkspace.service.ClubAppWorkspaceService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClubAppWorkspaceServiceTest {

    private ClubAppWorkspaceGroupRepository groupRepository;
    private ClubAppWorkspaceProjectRepository projectRepository;
    private ClubAppWorkspaceProjectMaterialRepository materialRepository;
    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private CurrentUserProvider currentUserProvider;
    private ClubAppWorkspaceObjectStorageService objectStorageService;
    private ClubAppWorkspaceService service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(ClubAppWorkspaceGroupRepository.class);
        projectRepository = mock(ClubAppWorkspaceProjectRepository.class);
        materialRepository = mock(ClubAppWorkspaceProjectMaterialRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        objectStorageService = mock(ClubAppWorkspaceObjectStorageService.class);

        service = new ClubAppWorkspaceService(
                groupRepository,
                projectRepository,
                materialRepository,
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                currentUserProvider,
                objectStorageService
        );

        when(clubRepository.findById(9L)).thenReturn(Optional.of(club(9L, "Xiuyuan Club")));
    }

    @Test
    void getVisibleWorkspaceShouldFilterDisabledItemsForStudent() {
        ClubAppWorkspaceGroupEntity enabledGroup = group(11L, 9L, "Innovation Contest", true, 1);
        ClubAppWorkspaceProjectEntity enabledProject = project(31L, 11L, "Cycling Guardian", "cycling-guardian", true, 1);

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L))
                .thenReturn(List.of(enabledGroup));
        when(projectRepository.findAllByGroupIdInAndEnabledTrueOrderBySortOrderAscIdAsc(List.of(11L)))
                .thenReturn(List.of(enabledProject));

        var response = service.getVisibleWorkspace(9L);

        assertEquals(1, response.groups().size());
        assertEquals("Innovation Contest", response.groups().get(0).title());
        assertEquals(1, response.groups().get(0).projects().size());
        assertEquals("Cycling Guardian", response.groups().get(0).projects().get(0).title());
        assertEquals("cycling-guardian", response.groups().get(0).projects().get(0).projectKey());
    }

    @Test
    void getVisibleProjectDetailShouldReturnProtectedDownloadRoute() {
        ClubAppWorkspaceGroupEntity enabledGroup = group(11L, 9L, "Innovation Contest", true, 1);
        ClubAppWorkspaceProjectEntity enabledProject = project(31L, 11L, "Cycling Guardian", "cycling-guardian", true, 1);
        enabledProject.setSummary("Project summary");
        enabledProject.setCoverUrl("https://example.com/cover.png");
        ClubAppWorkspaceProjectMaterialEntity material = material(
                41L,
                31L,
                "REPORT",
                "Research Report",
                "projects/cycling-guardian/report/research-report.docx",
                true,
                1
        );

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L))
                .thenReturn(List.of(enabledGroup));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "cycling-guardian"))
                .thenReturn(Optional.of(enabledProject));
        when(materialRepository.findAllByProjectIdAndEnabledTrueOrderBySortOrderAscIdAsc(31L))
                .thenReturn(List.of(material));

        var response = service.getVisibleProjectDetail(9L, "cycling-guardian");

        assertEquals("cycling-guardian", response.projectKey());
        assertEquals("Project summary", response.summary());
        assertEquals("PLACEHOLDER", response.demoMode());
        assertEquals(1, response.materials().size());
        assertEquals("Research Report", response.materials().get(0).title());
        assertEquals(
                "/api/v1/clubs/9/app-workspace/projects/cycling-guardian/materials/41/download",
                response.materials().get(0).downloadUrl()
        );
    }

    @Test
    void downloadVisibleMaterialShouldUseProtectedStorageService() {
        ClubAppWorkspaceGroupEntity enabledGroup = group(11L, 9L, "Innovation Contest", true, 1);
        ClubAppWorkspaceProjectEntity enabledProject = project(31L, 11L, "Cycling Guardian", "cycling-guardian", true, 1);
        ClubAppWorkspaceProjectMaterialEntity material = material(
                41L,
                31L,
                "REPORT",
                "Research Report",
                "projects/cycling-guardian/report/research-report.docx",
                true,
                1
        );
        ClubAppWorkspaceMaterialContent content = new ClubAppWorkspaceMaterialContent(
                new ByteArrayInputStream("demo".getBytes(StandardCharsets.UTF_8)),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                4,
                "research-report.docx"
        );

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(true);
        when(groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(9L))
                .thenReturn(List.of(enabledGroup));
        when(projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(List.of(11L), "cycling-guardian"))
                .thenReturn(Optional.of(enabledProject));
        when(materialRepository.findFirstByIdAndProjectIdAndEnabledTrue(41L, 31L))
                .thenReturn(Optional.of(material));
        when(objectStorageService.download("projects/cycling-guardian/report/research-report.docx"))
                .thenReturn(content);

        ClubAppWorkspaceMaterialContent response = service.downloadVisibleMaterial(9L, "cycling-guardian", 41L);

        assertEquals("research-report.docx", response.fileName());
        verify(objectStorageService).download("projects/cycling-guardian/report/research-report.docx");
    }

    @Test
    void saveManagerWorkspaceShouldNormalizeOrderingAndDeleteStaleItems() {
        ClubAppWorkspaceGroupEntity existingGroup = group(11L, 9L, "Legacy Group", true, 4);
        ClubAppWorkspaceProjectEntity existingProject = project(31L, 11L, "Legacy Project", "cycling-guardian", true, 7);
        ClubAppWorkspaceProjectMaterialEntity existingMaterial = material(
                41L,
                31L,
                "REPORT",
                "Legacy Material",
                "projects/cycling-guardian/report/legacy.docx",
                true,
                9
        );

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(9L, 5L)).thenReturn(true);
        when(groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(9L))
                .thenReturn(List.of(existingGroup), List.of(existingGroup));
        when(projectRepository.findAllByGroupIdInOrderBySortOrderAscIdAsc(List.of(11L)))
                .thenReturn(List.of(existingProject), List.of(existingProject));
        when(materialRepository.findAllByProjectIdInOrderBySortOrderAscIdAsc(List.of(31L)))
                .thenReturn(List.of(existingMaterial), List.of(existingMaterial));
        when(groupRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.saveManagerWorkspace(
                9L,
                new ClubAppWorkspaceRequest(List.of(
                        new ClubAppWorkspaceGroupRequest(
                                11L,
                                "Innovation Contest",
                                "",
                                true,
                                List.of(
                                        new ClubAppWorkspaceProjectRequest(
                                                31L,
                                                "cycling-guardian",
                                                "Cycling Guardian",
                                                "AI safety and health system",
                                                "Project summary",
                                                "https://example.com/ride.png",
                                                true,
                                                List.of(
                                                        new ClubAppWorkspaceProjectMaterialRequest(
                                                                41L,
                                                                "REPORT",
                                                                "Research Report",
                                                                "projects/cycling-guardian/report/research-report.docx",
                                                                true
                                                        )
                                                )
                                        )
                                )
                        )
                ))
        );

        assertEquals(1, response.groups().size());
        assertEquals(1, existingGroup.getSortOrder());
        assertEquals(1, existingProject.getSortOrder());
        assertEquals(1, existingMaterial.getSortOrder());
        assertEquals("Innovation Contest", existingGroup.getTitle());
        assertEquals("cycling-guardian", existingProject.getProjectKey());
        assertEquals("Project summary", existingProject.getSummary());
        assertEquals("https://example.com/ride.png", existingProject.getCoverUrl());
        assertEquals("Research Report", existingMaterial.getTitle());
        assertEquals("projects/cycling-guardian/report/research-report.docx", existingMaterial.getStoragePath());
    }

    @Test
    void saveManagerWorkspaceShouldRejectRelativeCoverUrl() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(9L, 5L)).thenReturn(true);
        when(groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveManagerWorkspace(
                        9L,
                        new ClubAppWorkspaceRequest(List.of(
                                new ClubAppWorkspaceGroupRequest(
                                        null,
                                        "Innovation Contest",
                                        "",
                                        true,
                                        List.of(
                                                new ClubAppWorkspaceProjectRequest(
                                                        null,
                                                        "cycling-guardian",
                                                        "Cycling Guardian",
                                                        "AI safety and health system",
                                                        "Project summary",
                                                        "/relative/cover.png",
                                                        true,
                                                        List.of()
                                                )
                                        )
                                )
                        ))
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void saveManagerWorkspaceShouldRejectAbsoluteMaterialUrl() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(9L, 5L)).thenReturn(true);
        when(groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveManagerWorkspace(
                        9L,
                        new ClubAppWorkspaceRequest(List.of(
                                new ClubAppWorkspaceGroupRequest(
                                        null,
                                        "Innovation Contest",
                                        "",
                                        true,
                                        List.of(
                                                new ClubAppWorkspaceProjectRequest(
                                                        null,
                                                        "cycling-guardian",
                                                        "Cycling Guardian",
                                                        "AI safety and health system",
                                                        "Project summary",
                                                        "https://example.com/ride.png",
                                                        true,
                                                        List.of(
                                                                new ClubAppWorkspaceProjectMaterialRequest(
                                                                        null,
                                                                        "REPORT",
                                                                        "Research Report",
                                                                        "https://bucket.example.com/report.docx",
                                                                        true
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ))
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void getVisibleWorkspaceShouldRejectUnauthorizedStudent() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(15L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(9L, 15L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getVisibleWorkspace(9L));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(groupRepository, never()).findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(any());
    }

    private ClubEntity club(Long id, String name) {
        ClubEntity club = new ClubEntity();
        setField(ClubEntity.class, club, "id", id);
        club.setName(name);
        return club;
    }

    private ClubAppWorkspaceGroupEntity group(Long id, Long clubId, String title, boolean enabled, int sortOrder) {
        ClubAppWorkspaceGroupEntity group = new ClubAppWorkspaceGroupEntity();
        setField(ClubAppWorkspaceGroupEntity.class, group, "id", id);
        group.setClubId(clubId);
        group.setTitle(title);
        group.setSubtitle("");
        group.setEnabled(enabled);
        group.setSortOrder(sortOrder);
        group.setCreatedBy(5L);
        group.setUpdatedBy(5L);
        return group;
    }

    private ClubAppWorkspaceProjectEntity project(
            Long id,
            Long groupId,
            String title,
            String projectKey,
            boolean enabled,
            int sortOrder
    ) {
        ClubAppWorkspaceProjectEntity project = new ClubAppWorkspaceProjectEntity();
        setField(ClubAppWorkspaceProjectEntity.class, project, "id", id);
        project.setGroupId(groupId);
        project.setProjectKey(projectKey);
        project.setTitle(title);
        project.setSubtitle("");
        project.setSummary("");
        project.setEnabled(enabled);
        project.setSortOrder(sortOrder);
        project.setCreatedBy(5L);
        project.setUpdatedBy(5L);
        return project;
    }

    private ClubAppWorkspaceProjectMaterialEntity material(
            Long id,
            Long projectId,
            String sectionKey,
            String title,
            String storagePath,
            boolean enabled,
            int sortOrder
    ) {
        ClubAppWorkspaceProjectMaterialEntity material = new ClubAppWorkspaceProjectMaterialEntity();
        setField(ClubAppWorkspaceProjectMaterialEntity.class, material, "id", id);
        material.setProjectId(projectId);
        material.setSectionKey(sectionKey);
        material.setTitle(title);
        material.setStoragePath(storagePath);
        material.setEnabled(enabled);
        material.setSortOrder(sortOrder);
        material.setCreatedBy(5L);
        material.setUpdatedBy(5L);
        return material;
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
