package com.scms.core.clubappworkspace.service;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceGroupEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectEntity;
import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectMaterialEntity;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceGroupRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceGroupResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectDetailResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectMaterialRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectMaterialResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceProjectResponse;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceRequest;
import com.scms.core.clubappworkspace.dto.ClubAppWorkspaceResponse;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceGroupResponse;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectMaterialResponse;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceProjectResponse;
import com.scms.core.clubappworkspace.dto.ManagerClubAppWorkspaceResponse;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceGroupRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectMaterialRepository;
import com.scms.core.clubappworkspace.repository.ClubAppWorkspaceProjectRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ClubAppWorkspaceService {

    private static final Set<String> MATERIAL_SECTION_KEYS = Set.of(
            "OVERVIEW",
            "REPORT",
            "ATTACHMENTS",
            "IMAGES",
            "POSTER",
            "VIDEOS",
            "OTHER"
    );

    private static final Pattern PROJECT_KEY_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final ClubAppWorkspaceGroupRepository groupRepository;
    private final ClubAppWorkspaceProjectRepository projectRepository;
    private final ClubAppWorkspaceProjectMaterialRepository materialRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ClubAppWorkspaceObjectStorageService objectStorageService;

    public ClubAppWorkspaceService(ClubAppWorkspaceGroupRepository groupRepository,
                                   ClubAppWorkspaceProjectRepository projectRepository,
                                   ClubAppWorkspaceProjectMaterialRepository materialRepository,
                                   ClubRepository clubRepository,
                                   ClubManagerBindingRepository clubManagerBindingRepository,
                                   ClubStudentMemberRepository clubStudentMemberRepository,
                                   CurrentUserProvider currentUserProvider,
                                   ClubAppWorkspaceObjectStorageService objectStorageService) {
        this.groupRepository = groupRepository;
        this.projectRepository = projectRepository;
        this.materialRepository = materialRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectStorageService = objectStorageService;
    }

    @Transactional(readOnly = true)
    public ClubAppWorkspaceResponse getVisibleWorkspace(Long clubId) {
        requireViewerAccessToClub(clubId);
        ClubEntity club = getRequiredClub(clubId);
        List<ClubAppWorkspaceGroupEntity> groups =
                groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(clubId);
        Map<Long, List<ClubAppWorkspaceProjectEntity>> projectMap = loadProjectMap(groups, true);

        return new ClubAppWorkspaceResponse(
                club.getId(),
                club.getName(),
                groups.stream()
                        .map(group -> new ClubAppWorkspaceGroupResponse(
                                group.getId(),
                                group.getTitle(),
                                group.getSubtitle(),
                                group.getSortOrder(),
                                projectMap.getOrDefault(group.getId(), List.of()).stream()
                                        .map(project -> new ClubAppWorkspaceProjectResponse(
                                                project.getId(),
                                                project.getProjectKey(),
                                                project.getTitle(),
                                                project.getSubtitle(),
                                                project.getCoverUrl(),
                                                project.getSortOrder()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public ClubAppWorkspaceProjectDetailResponse getVisibleProjectDetail(Long clubId, String projectKey) {
        requireViewerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredVisibleProject(clubId, projectKey);

        List<ClubAppWorkspaceProjectMaterialEntity> materials =
                materialRepository.findAllByProjectIdAndEnabledTrueOrderBySortOrderAscIdAsc(project.getId());

        return new ClubAppWorkspaceProjectDetailResponse(
                project.getId(),
                project.getProjectKey(),
                project.getTitle(),
                project.getSubtitle(),
                project.getCoverUrl(),
                project.getSummary(),
                materials.stream()
                        .map(material -> new ClubAppWorkspaceProjectMaterialResponse(
                                material.getId(),
                                material.getSectionKey(),
                                material.getTitle(),
                                material.getStoragePath(),
                                buildProtectedDownloadUrl(clubId, project.getProjectKey(), material.getId()),
                                material.getSortOrder()
                        ))
                        .toList(),
                "PLACEHOLDER"
        );
    }

    @Transactional(readOnly = true)
    public ClubAppWorkspaceMaterialContent downloadVisibleMaterial(Long clubId, String projectKey, Long materialId) {
        requireViewerAccessToClub(clubId);
        ClubAppWorkspaceProjectEntity project = getRequiredVisibleProject(clubId, projectKey);
        ClubAppWorkspaceProjectMaterialEntity material = materialRepository
                .findFirstByIdAndProjectIdAndEnabledTrue(materialId, project.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "workspace material not found"));
        return objectStorageService.download(material.getStoragePath());
    }

    @Transactional(readOnly = true)
    public ManagerClubAppWorkspaceResponse getManagerWorkspace(Long clubId) {
        requireManagerAccessToClub(clubId);
        ClubEntity club = getRequiredClub(clubId);
        List<ClubAppWorkspaceGroupEntity> groups = groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(clubId);
        Map<Long, List<ClubAppWorkspaceProjectEntity>> projectMap = loadProjectMap(groups, false);
        Map<Long, List<ClubAppWorkspaceProjectMaterialEntity>> materialMap = loadMaterialMap(projectMap.values(), false);
        return toManagerResponse(club, groups, projectMap, materialMap);
    }

    @Transactional
    public ManagerClubAppWorkspaceResponse saveManagerWorkspace(Long clubId, ClubAppWorkspaceRequest request) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        ClubEntity club = getRequiredClub(clubId);
        List<ClubAppWorkspaceGroupEntity> existingGroups =
                groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(clubId);
        Map<Long, ClubAppWorkspaceGroupEntity> existingGroupMap = toGroupMap(existingGroups);
        Map<Long, List<ClubAppWorkspaceProjectEntity>> existingProjectMap = loadProjectMap(existingGroups, false);
        Map<Long, List<ClubAppWorkspaceProjectMaterialEntity>> existingMaterialMap =
                loadMaterialMap(existingProjectMap.values(), false);
        List<NormalizedGroupInput> normalizedGroups = normalizeGroups(request);

        if (normalizedGroups.isEmpty()) {
            if (!existingGroups.isEmpty()) {
                groupRepository.deleteAll(existingGroups);
            }
            return new ManagerClubAppWorkspaceResponse(club.getId(), club.getName(), List.of());
        }

        LinkedHashSet<Long> keptExistingGroupIds = new LinkedHashSet<>();
        List<ClubAppWorkspaceGroupEntity> groupsToSave = new ArrayList<>();
        for (int index = 0; index < normalizedGroups.size(); index++) {
            NormalizedGroupInput input = normalizedGroups.get(index);
            ClubAppWorkspaceGroupEntity entity = input.id() == null
                    ? new ClubAppWorkspaceGroupEntity()
                    : requireExistingGroup(existingGroupMap, input.id());
            if (input.id() != null) {
                keptExistingGroupIds.add(input.id());
            }
            entity.setClubId(clubId);
            entity.setTitle(input.title());
            entity.setSubtitle(input.subtitle());
            entity.setEnabled(input.enabled());
            entity.setSortOrder(index + 1);
            if (input.id() == null) {
                entity.setCreatedBy(manager.userId());
            }
            entity.setUpdatedBy(manager.userId());
            groupsToSave.add(entity);
        }

        List<ClubAppWorkspaceGroupEntity> savedGroups = groupRepository.saveAll(groupsToSave);

        List<Long> staleGroupIds = existingGroups.stream()
                .map(ClubAppWorkspaceGroupEntity::getId)
                .filter(existingId -> !keptExistingGroupIds.contains(existingId))
                .toList();
        if (!staleGroupIds.isEmpty()) {
            groupRepository.deleteAllByIdInBatch(staleGroupIds);
        }

        for (int groupIndex = 0; groupIndex < savedGroups.size(); groupIndex++) {
            ClubAppWorkspaceGroupEntity savedGroup = savedGroups.get(groupIndex);
            NormalizedGroupInput input = normalizedGroups.get(groupIndex);
            List<ClubAppWorkspaceProjectEntity> currentExistingProjects =
                    input.id() == null ? List.of() : existingProjectMap.getOrDefault(input.id(), List.of());
            Map<Long, ClubAppWorkspaceProjectEntity> currentProjectMap = toProjectMap(currentExistingProjects);

            LinkedHashSet<Long> keptExistingProjectIds = new LinkedHashSet<>();
            List<ClubAppWorkspaceProjectEntity> projectsToSave = new ArrayList<>();
            for (int projectIndex = 0; projectIndex < input.projects().size(); projectIndex++) {
                NormalizedProjectInput projectInput = input.projects().get(projectIndex);
                ClubAppWorkspaceProjectEntity projectEntity = projectInput.id() == null
                        ? new ClubAppWorkspaceProjectEntity()
                        : requireExistingProject(currentProjectMap, projectInput.id());
                if (projectInput.id() != null) {
                    keptExistingProjectIds.add(projectInput.id());
                }
                projectEntity.setGroupId(savedGroup.getId());
                projectEntity.setProjectKey(projectInput.projectKey());
                projectEntity.setTitle(projectInput.title());
                projectEntity.setSubtitle(projectInput.subtitle());
                projectEntity.setSummary(projectInput.summary());
                projectEntity.setCoverUrl(projectInput.coverUrl());
                projectEntity.setEnabled(projectInput.enabled());
                projectEntity.setSortOrder(projectIndex + 1);
                if (projectInput.id() == null) {
                    projectEntity.setCreatedBy(manager.userId());
                }
                projectEntity.setUpdatedBy(manager.userId());
                projectsToSave.add(projectEntity);
            }

            List<Long> staleProjectIds = currentExistingProjects.stream()
                    .map(ClubAppWorkspaceProjectEntity::getId)
                    .filter(existingId -> !keptExistingProjectIds.contains(existingId))
                    .toList();
            if (!staleProjectIds.isEmpty()) {
                projectRepository.deleteAllByIdInBatch(staleProjectIds);
            }

            List<ClubAppWorkspaceProjectEntity> savedProjects = projectsToSave.isEmpty()
                    ? List.of()
                    : projectRepository.saveAll(projectsToSave);

            for (int projectIndex = 0; projectIndex < savedProjects.size(); projectIndex++) {
                ClubAppWorkspaceProjectEntity savedProject = savedProjects.get(projectIndex);
                NormalizedProjectInput projectInput = input.projects().get(projectIndex);
                List<ClubAppWorkspaceProjectMaterialEntity> currentExistingMaterials =
                        projectInput.id() == null ? List.of() : existingMaterialMap.getOrDefault(projectInput.id(), List.of());
                Map<Long, ClubAppWorkspaceProjectMaterialEntity> currentMaterialMap = toMaterialMap(currentExistingMaterials);

                LinkedHashSet<Long> keptExistingMaterialIds = new LinkedHashSet<>();
                List<ClubAppWorkspaceProjectMaterialEntity> materialsToSave = new ArrayList<>();
                for (int materialIndex = 0; materialIndex < projectInput.materials().size(); materialIndex++) {
                    NormalizedMaterialInput materialInput = projectInput.materials().get(materialIndex);
                    ClubAppWorkspaceProjectMaterialEntity materialEntity = materialInput.id() == null
                            ? new ClubAppWorkspaceProjectMaterialEntity()
                            : requireExistingMaterial(currentMaterialMap, materialInput.id());
                    if (materialInput.id() != null) {
                        keptExistingMaterialIds.add(materialInput.id());
                    }
                    materialEntity.setProjectId(savedProject.getId());
                    materialEntity.setSectionKey(materialInput.sectionKey());
                    materialEntity.setTitle(materialInput.title());
                    materialEntity.setStoragePath(materialInput.storagePath());
                    materialEntity.setEnabled(materialInput.enabled());
                    materialEntity.setSortOrder(materialIndex + 1);
                    if (materialInput.id() == null) {
                        materialEntity.setCreatedBy(manager.userId());
                    }
                    materialEntity.setUpdatedBy(manager.userId());
                    materialsToSave.add(materialEntity);
                }

                List<Long> staleMaterialIds = currentExistingMaterials.stream()
                        .map(ClubAppWorkspaceProjectMaterialEntity::getId)
                        .filter(existingId -> !keptExistingMaterialIds.contains(existingId))
                        .toList();
                if (!staleMaterialIds.isEmpty()) {
                    materialRepository.deleteAllByIdInBatch(staleMaterialIds);
                }
                if (!materialsToSave.isEmpty()) {
                    materialRepository.saveAll(materialsToSave);
                }
            }
        }

        List<ClubAppWorkspaceGroupEntity> refreshedGroups =
                groupRepository.findAllByClubIdOrderBySortOrderAscIdAsc(clubId);
        Map<Long, List<ClubAppWorkspaceProjectEntity>> refreshedProjectMap = loadProjectMap(refreshedGroups, false);
        Map<Long, List<ClubAppWorkspaceProjectMaterialEntity>> refreshedMaterialMap =
                loadMaterialMap(refreshedProjectMap.values(), false);
        return toManagerResponse(club, refreshedGroups, refreshedProjectMap, refreshedMaterialMap);
    }

    private ManagerClubAppWorkspaceResponse toManagerResponse(ClubEntity club,
                                                              List<ClubAppWorkspaceGroupEntity> groups,
                                                              Map<Long, List<ClubAppWorkspaceProjectEntity>> projectMap,
                                                              Map<Long, List<ClubAppWorkspaceProjectMaterialEntity>> materialMap) {
        return new ManagerClubAppWorkspaceResponse(
                club.getId(),
                club.getName(),
                groups.stream()
                        .map(group -> new ManagerClubAppWorkspaceGroupResponse(
                                group.getId(),
                                group.getTitle(),
                                group.getSubtitle(),
                                group.getSortOrder(),
                                Boolean.TRUE.equals(group.getEnabled()),
                                projectMap.getOrDefault(group.getId(), List.of()).stream()
                                        .map(project -> new ManagerClubAppWorkspaceProjectResponse(
                                                project.getId(),
                                                project.getProjectKey(),
                                                project.getTitle(),
                                                project.getSubtitle(),
                                                project.getSummary(),
                                                project.getCoverUrl(),
                                                project.getSortOrder(),
                                                Boolean.TRUE.equals(project.getEnabled()),
                                                materialMap.getOrDefault(project.getId(), List.of()).stream()
                                                        .map(material -> new ManagerClubAppWorkspaceProjectMaterialResponse(
                                                                material.getId(),
                                                                material.getSectionKey(),
                                                                material.getTitle(),
                                                                material.getStoragePath(),
                                                                material.getSortOrder(),
                                                                Boolean.TRUE.equals(material.getEnabled())
                                                        ))
                                                        .toList()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    private List<NormalizedGroupInput> normalizeGroups(ClubAppWorkspaceRequest request) {
        List<ClubAppWorkspaceGroupRequest> rawGroups = request == null || request.groups() == null
                ? List.of()
                : request.groups();
        LinkedHashSet<Long> seenGroupIds = new LinkedHashSet<>();
        LinkedHashSet<String> seenProjectKeys = new LinkedHashSet<>();
        List<NormalizedGroupInput> result = new ArrayList<>();
        for (ClubAppWorkspaceGroupRequest group : rawGroups) {
            Long groupId = normalizeId(group == null ? null : group.id());
            if (groupId != null && !seenGroupIds.add(groupId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "duplicate workspace group ids");
            }
            result.add(new NormalizedGroupInput(
                    groupId,
                    requireTitle(group == null ? null : group.title(), "workspace group title is required"),
                    normalizeText(group == null ? null : group.subtitle()),
                    normalizeEnabled(group == null ? null : group.enabled()),
                    normalizeProjects(group == null ? null : group.projects(), seenProjectKeys)
            ));
        }
        return result;
    }

    private List<NormalizedProjectInput> normalizeProjects(List<ClubAppWorkspaceProjectRequest> rawProjects,
                                                           Set<String> seenProjectKeys) {
        if (rawProjects == null || rawProjects.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> seenProjectIds = new LinkedHashSet<>();
        List<NormalizedProjectInput> result = new ArrayList<>();
        for (ClubAppWorkspaceProjectRequest project : rawProjects) {
            Long projectId = normalizeId(project == null ? null : project.id());
            if (projectId != null && !seenProjectIds.add(projectId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "duplicate workspace project ids");
            }

            String projectKey = requireProjectKey(project == null ? null : project.projectKey());
            if (!seenProjectKeys.add(projectKey)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project key must be unique");
            }

            result.add(new NormalizedProjectInput(
                    projectId,
                    projectKey,
                    requireTitle(project == null ? null : project.title(), "workspace project title is required"),
                    normalizeText(project == null ? null : project.subtitle()),
                    normalizeSummary(project == null ? null : project.summary()),
                    normalizeCoverUrl(project == null ? null : project.coverUrl()),
                    normalizeEnabled(project == null ? null : project.enabled()),
                    normalizeMaterials(project == null ? null : project.materials())
            ));
        }
        return result;
    }

    private List<NormalizedMaterialInput> normalizeMaterials(List<ClubAppWorkspaceProjectMaterialRequest> rawMaterials) {
        if (rawMaterials == null || rawMaterials.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> seenMaterialIds = new LinkedHashSet<>();
        List<NormalizedMaterialInput> result = new ArrayList<>();
        for (ClubAppWorkspaceProjectMaterialRequest material : rawMaterials) {
            Long materialId = normalizeId(material == null ? null : material.id());
            if (materialId != null && !seenMaterialIds.add(materialId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "duplicate workspace material ids");
            }
            result.add(new NormalizedMaterialInput(
                    materialId,
                    normalizeSectionKey(material == null ? null : material.sectionKey()),
                    requireTitle(material == null ? null : material.title(), "workspace material title is required"),
                    normalizeStoragePath(material == null ? null : material.storagePath()),
                    normalizeEnabled(material == null ? null : material.enabled())
            ));
        }
        return result;
    }

    private Map<Long, List<ClubAppWorkspaceProjectEntity>> loadProjectMap(Collection<ClubAppWorkspaceGroupEntity> groups,
                                                                          boolean visibleOnly) {
        if (groups == null || groups.isEmpty()) {
            return Map.of();
        }
        List<Long> groupIds = groups.stream()
                .map(ClubAppWorkspaceGroupEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (groupIds.isEmpty()) {
            return Map.of();
        }

        List<ClubAppWorkspaceProjectEntity> projects = visibleOnly
                ? projectRepository.findAllByGroupIdInAndEnabledTrueOrderBySortOrderAscIdAsc(groupIds)
                : projectRepository.findAllByGroupIdInOrderBySortOrderAscIdAsc(groupIds);

        Map<Long, List<ClubAppWorkspaceProjectEntity>> result = new LinkedHashMap<>();
        for (Long groupId : groupIds) {
            result.put(groupId, new ArrayList<>());
        }
        for (ClubAppWorkspaceProjectEntity project : projects) {
            result.computeIfAbsent(project.getGroupId(), ignored -> new ArrayList<>()).add(project);
        }
        return result;
    }

    private Map<Long, List<ClubAppWorkspaceProjectMaterialEntity>> loadMaterialMap(Collection<List<ClubAppWorkspaceProjectEntity>> projectLists,
                                                                                   boolean visibleOnly) {
        if (projectLists == null || projectLists.isEmpty()) {
            return Map.of();
        }

        List<Long> projectIds = projectLists.stream()
                .flatMap(Collection::stream)
                .map(ClubAppWorkspaceProjectEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (projectIds.isEmpty()) {
            return Map.of();
        }

        List<ClubAppWorkspaceProjectMaterialEntity> materials = visibleOnly
                ? materialRepository.findAllByProjectIdInAndEnabledTrueOrderBySortOrderAscIdAsc(projectIds)
                : materialRepository.findAllByProjectIdInOrderBySortOrderAscIdAsc(projectIds);

        Map<Long, List<ClubAppWorkspaceProjectMaterialEntity>> result = new LinkedHashMap<>();
        for (Long projectId : projectIds) {
            result.put(projectId, new ArrayList<>());
        }
        for (ClubAppWorkspaceProjectMaterialEntity material : materials) {
            result.computeIfAbsent(material.getProjectId(), ignored -> new ArrayList<>()).add(material);
        }
        return result;
    }

    private Map<Long, ClubAppWorkspaceGroupEntity> toGroupMap(List<ClubAppWorkspaceGroupEntity> groups) {
        Map<Long, ClubAppWorkspaceGroupEntity> result = new LinkedHashMap<>();
        for (ClubAppWorkspaceGroupEntity group : groups) {
            result.put(group.getId(), group);
        }
        return result;
    }

    private Map<Long, ClubAppWorkspaceProjectEntity> toProjectMap(List<ClubAppWorkspaceProjectEntity> projects) {
        Map<Long, ClubAppWorkspaceProjectEntity> result = new LinkedHashMap<>();
        for (ClubAppWorkspaceProjectEntity project : projects) {
            result.put(project.getId(), project);
        }
        return result;
    }

    private Map<Long, ClubAppWorkspaceProjectMaterialEntity> toMaterialMap(List<ClubAppWorkspaceProjectMaterialEntity> materials) {
        Map<Long, ClubAppWorkspaceProjectMaterialEntity> result = new LinkedHashMap<>();
        for (ClubAppWorkspaceProjectMaterialEntity material : materials) {
            result.put(material.getId(), material);
        }
        return result;
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private ClubAppWorkspaceProjectEntity getRequiredVisibleProject(Long clubId, String projectKey) {
        List<ClubAppWorkspaceGroupEntity> groups =
                groupRepository.findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(clubId);
        List<Long> groupIds = groups.stream()
                .map(ClubAppWorkspaceGroupEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (groupIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "workspace project not found");
        }

        String normalizedProjectKey = requireProjectKey(projectKey);
        return projectRepository.findFirstByGroupIdInAndProjectKeyAndEnabledTrue(groupIds, normalizedProjectKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "workspace project not found"));
    }

    private AuthenticatedUser requireViewerAccessToClub(Long clubId) {
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
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager role required");
        }
        if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager permission denied");
        }
        return user;
    }

    private ClubAppWorkspaceGroupEntity requireExistingGroup(Map<Long, ClubAppWorkspaceGroupEntity> existingGroupMap, Long groupId) {
        ClubAppWorkspaceGroupEntity entity = existingGroupMap.get(groupId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace group is invalid");
        }
        return entity;
    }

    private ClubAppWorkspaceProjectEntity requireExistingProject(Map<Long, ClubAppWorkspaceProjectEntity> existingProjectMap,
                                                                 Long projectId) {
        ClubAppWorkspaceProjectEntity entity = existingProjectMap.get(projectId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project is invalid");
        }
        return entity;
    }

    private ClubAppWorkspaceProjectMaterialEntity requireExistingMaterial(Map<Long, ClubAppWorkspaceProjectMaterialEntity> existingMaterialMap,
                                                                          Long materialId) {
        ClubAppWorkspaceProjectMaterialEntity entity = existingMaterialMap.get(materialId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace material is invalid");
        }
        return entity;
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

    private String requireTitle(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return normalized;
    }

    private String requireProjectKey(String value) {
        String normalized = normalizeText(value).toLowerCase();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project key is required");
        }
        if (!PROJECT_KEY_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project key is invalid");
        }
        return normalized;
    }

    private String normalizeSectionKey(String value) {
        String normalized = normalizeText(value).toUpperCase();
        if (!MATERIAL_SECTION_KEYS.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace material section is invalid");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSummary(String value) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace project summary is required");
        }
        return normalized;
    }

    private String normalizeCoverUrl(String value) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            return null;
        }
        validateAbsoluteUrl(normalized);
        return normalized;
    }

    private String normalizeStoragePath(String value) {
        String normalized = normalizeText(value).replace('\\', '/');
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace material storage path is required");
        }
        if (normalized.startsWith("/")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace material storage path must be relative");
        }
        if (normalized.contains("../") || normalized.contains("/..") || normalized.equals("..")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace material storage path is invalid");
        }
        try {
            URI uri = new URI(normalized);
            if (uri.isAbsolute()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace material storage path must be relative");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace material storage path is invalid");
        }
        return normalized;
    }

    private void validateAbsoluteUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!uri.isAbsolute()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace cover url must be absolute");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspace cover url is invalid");
        }
    }

    private String buildProtectedDownloadUrl(Long clubId, String projectKey, Long materialId) {
        return "/api/v1/clubs/" + clubId
                + "/app-workspace/projects/" + projectKey
                + "/materials/" + materialId
                + "/download";
    }

    private record NormalizedGroupInput(
            Long id,
            String title,
            String subtitle,
            boolean enabled,
            List<NormalizedProjectInput> projects
    ) {
    }

    private record NormalizedProjectInput(
            Long id,
            String projectKey,
            String title,
            String subtitle,
            String summary,
            String coverUrl,
            boolean enabled,
            List<NormalizedMaterialInput> materials
    ) {
    }

    private record NormalizedMaterialInput(
            Long id,
            String sectionKey,
            String title,
            String storagePath,
            boolean enabled
    ) {
    }
}
