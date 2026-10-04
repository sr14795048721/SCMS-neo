package com.scms.core.club.service;

import com.scms.core.adminuser.dto.AdminManagerOptionResponse;
import com.scms.core.adminuser.dto.AdminUserPageResponse;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubManagerBindingEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.AdminClubDetailResponse;
import com.scms.core.club.dto.AdminClubListItemResponse;
import com.scms.core.club.dto.AdminClubManagerResponse;
import com.scms.core.club.dto.AdminClubMutationRequest;
import com.scms.core.club.dto.AdminClubOptionResponse;
import com.scms.core.club.dto.AdminClubStudentResponse;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRelationCountProjection;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.homeclubrecommend.service.HomeClubRecommendationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminClubService {

    private static final int MAX_CLUB_NAME_LENGTH = 12;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_OPTION_LIMIT = 20;

    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final UserRepository userRepository;
    private final ManagerInfoRepository managerInfoRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final ActivityRepository activityRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final HomeClubRecommendationService homeClubRecommendationService;
    private final ClubDutySupport clubDutySupport;

    public AdminClubService(ClubRepository clubRepository,
                            ClubManagerBindingRepository clubManagerBindingRepository,
                            ClubStudentMemberRepository clubStudentMemberRepository,
                            UserRepository userRepository,
                            ManagerInfoRepository managerInfoRepository,
                            StudentInfoRepository studentInfoRepository,
                            ActivityRepository activityRepository,
                            CurrentUserProvider currentUserProvider,
                            AuditService auditService,
                            HomeClubRecommendationService homeClubRecommendationService,
                            ClubDutySupport clubDutySupport) {
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.userRepository = userRepository;
        this.managerInfoRepository = managerInfoRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.activityRepository = activityRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.homeClubRecommendationService = homeClubRecommendationService;
        this.clubDutySupport = clubDutySupport;
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse<AdminClubListItemResponse> list(int page, int pageSize, String keyword, String status) {
        requireAdmin();

        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        String keywordLike = normalizedKeyword.isEmpty() ? "%" : "%" + normalizedKeyword + "%";
        ClubStatus clubStatus = parseStatus(status, true);

        Page<ClubEntity> clubs = clubRepository.searchAdmin(
                normalizedKeyword,
                keywordLike,
                clubStatus,
                PageRequest.of(normalizedPage - 1, normalizedPageSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );

        List<Long> clubIds = clubs.getContent().stream().map(ClubEntity::getId).toList();
        Map<Long, Long> memberCounts = loadStudentCounts(clubIds);
        Map<Long, Long> managerCounts = loadManagerCounts(clubIds);

        return new AdminUserPageResponse<>(
                clubs.getContent().stream()
                        .map(club -> toListItem(club, memberCounts, managerCounts))
                        .toList(),
                normalizedPage,
                normalizedPageSize,
                clubs.getTotalElements(),
                clubs.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminClubDetailResponse getDetail(Long clubId) {
        requireAdmin();
        ClubEntity club = getRequiredClub(clubId);
        return buildDetail(club);
    }

    @Transactional(readOnly = true)
    public List<AdminClubOptionResponse> listOptions(String keyword) {
        requireAdmin();

        String normalizedKeyword = normalizeKeyword(keyword);
        String keywordLike = normalizedKeyword.isEmpty() ? "%" : "%" + normalizedKeyword + "%";
        List<ClubEntity> clubs = clubRepository.searchAdmin(
                        normalizedKeyword,
                        keywordLike,
                        ClubStatus.ACTIVE,
                        PageRequest.of(0, 20, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")))
                )
                .getContent();
        Map<Long, Long> memberCounts = loadStudentCounts(clubs.stream().map(ClubEntity::getId).toList());

        return clubs.stream()
                .map(club -> new AdminClubOptionResponse(
                        club.getId(),
                        club.getName(),
                        defaultString(club.getType()),
                        defaultString(club.getDescription()),
                        memberCounts.getOrDefault(club.getId(), 0L)
                ))
                .toList();
    }

    @Transactional
    public AdminClubDetailResponse create(AdminClubMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        String name = normalizeName(request.name());
        String type = normalizeType(request.type());
        String description = trimToNull(request.description());
        ClubStatus status = parseStatus(request.status(), false);
        List<Long> managerUserIds = normalizeManagerUserIds(request.managerUserIds());

        if (clubRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "club name already exists");
        }

        validateManagerUsers(managerUserIds);

        ClubEntity entity = new ClubEntity();
        entity.setName(name);
        entity.setType(type);
        entity.setStatus(status);
        entity.setDescription(description);
        entity.setCreatedBy(admin.userId());
        ClubEntity saved = clubRepository.save(entity);

        replaceManagerBindings(saved.getId(), managerUserIds);

        auditService.create("ADMIN_CLUB_CREATED", admin.userId(), "CLUB", String.valueOf(saved.getId()), "Administrator created a club");
        if (!managerUserIds.isEmpty()) {
            auditService.create("ADMIN_CLUB_MANAGERS_UPDATED", admin.userId(), "CLUB", String.valueOf(saved.getId()), "Administrator updated club manager bindings");
        }
        return buildDetail(saved);
    }

    @Transactional
    public AdminClubDetailResponse update(Long clubId, AdminClubMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        ClubEntity club = getRequiredClub(clubId);

        String name = normalizeName(request.name());
        String type = normalizeType(request.type());
        String description = trimToNull(request.description());
        ClubStatus status = parseStatus(request.status(), false);
        List<Long> managerUserIds = normalizeManagerUserIds(request.managerUserIds());

        if (clubRepository.existsByNameIgnoreCaseAndIdNot(name, clubId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "club name already exists");
        }

        validateManagerUsers(managerUserIds);
        List<ClubManagerBindingEntity> existingBindings = clubManagerBindingRepository.findAllByClubId(clubId);
        Set<Long> existingManagerIds = existingBindings.stream()
                .map(ClubManagerBindingEntity::getManagerUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        club.setName(name);
        club.setType(type);
        club.setStatus(status);
        club.setDescription(description);
        clubRepository.save(club);
        if (status != ClubStatus.ACTIVE) {
            homeClubRecommendationService.removeRecommendationsForClub(clubId);
        }

        boolean managerBindingsChanged = syncManagerBindings(clubId, existingBindings, managerUserIds);

        auditService.create("ADMIN_CLUB_UPDATED", admin.userId(), "CLUB", String.valueOf(clubId), "Administrator updated a club");
        if (managerBindingsChanged || !existingManagerIds.equals(new LinkedHashSet<>(managerUserIds))) {
            auditService.create("ADMIN_CLUB_MANAGERS_UPDATED", admin.userId(), "CLUB", String.valueOf(clubId), "Administrator updated club manager bindings");
        }
        return buildDetail(club);
    }

    @Transactional
    public void removeMember(Long clubId, Long studentUserId) {
        AuthenticatedUser admin = requireAdmin();
        getRequiredClub(clubId);
        ClubStudentMemberEntity member = clubStudentMemberRepository.findByClubIdAndStudentUserId(clubId, studentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club member not found"));

        clubStudentMemberRepository.delete(member);
        auditService.create(
                "ADMIN_CLUB_MEMBER_REMOVED",
                admin.userId(),
                "CLUB_MEMBER",
                String.valueOf(studentUserId),
                "Administrator removed a club member"
        );
    }

    @Transactional
    public void delete(Long clubId) {
        AuthenticatedUser admin = requireAdmin();
        ClubEntity club = getRequiredClub(clubId);

        if (activityRepository.countByClubId(clubId) > 0) {
            throw new BusinessException(ErrorCode.DEPENDENCY_EXISTS, "club still has related activities");
        }

        clubManagerBindingRepository.deleteAllByClubId(clubId);
        clubStudentMemberRepository.deleteAllByClubId(clubId);
        homeClubRecommendationService.removeRecommendationsForClub(clubId);
        clubRepository.delete(club);

        auditService.create("ADMIN_CLUB_DELETED", admin.userId(), "CLUB", String.valueOf(clubId), "Administrator deleted a club");
    }

    @Transactional(readOnly = true)
    public List<AdminManagerOptionResponse> listManagerOptions(String keyword) {
        requireAdmin();

        String normalizedKeyword = normalizeKeyword(keyword);
        List<UserEntity> users = userRepository.findAllByRole(UserRole.CLUB_MANAGER);
        Map<Long, ManagerInfoEntity> infoMap = managerInfoRepository.findAllByUserIdIn(users.stream().map(UserEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(ManagerInfoEntity::getUserId, item -> item));

        return users.stream()
                .map(user -> toManagerOption(user, infoMap.get(user.getId())))
                .filter(option -> matchesKeyword(option, normalizedKeyword))
                .sorted((left, right) -> {
                    String leftLabel = buildManagerOptionSortKey(left);
                    String rightLabel = buildManagerOptionSortKey(right);
                    return leftLabel.compareToIgnoreCase(rightLabel);
                })
                .limit(DEFAULT_OPTION_LIMIT)
                .toList();
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private AdminClubDetailResponse buildDetail(ClubEntity club) {
        List<AdminClubManagerResponse> managers = loadManagers(club.getId());
        List<AdminClubStudentResponse> students = loadStudents(club.getId());

        return new AdminClubDetailResponse(
                club.getId(),
                club.getName(),
                club.getType(),
                club.getStatus().name(),
                club.getDescription(),
                students.size(),
                managers.size(),
                club.getCreatedAt(),
                club.getUpdatedAt(),
                managers,
                students
        );
    }

    private AdminClubListItemResponse toListItem(ClubEntity club, Map<Long, Long> memberCounts, Map<Long, Long> managerCounts) {
        return new AdminClubListItemResponse(
                club.getId(),
                club.getName(),
                club.getType(),
                club.getStatus().name(),
                club.getDescription(),
                memberCounts.getOrDefault(club.getId(), 0L),
                managerCounts.getOrDefault(club.getId(), 0L),
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }

    private List<AdminClubManagerResponse> loadManagers(Long clubId) {
        List<Long> managerUserIds = clubManagerBindingRepository.findAllByClubId(clubId)
                .stream()
                .map(ClubManagerBindingEntity::getManagerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (managerUserIds.isEmpty()) {
            return List.of();
        }

        List<UserEntity> users = userRepository.findAllByIdInAndRole(managerUserIds, UserRole.CLUB_MANAGER);
        Map<Long, UserEntity> userMap = users.stream().collect(Collectors.toMap(UserEntity::getId, item -> item));
        Map<Long, ManagerInfoEntity> infoMap = managerInfoRepository.findAllByUserIdIn(managerUserIds)
                .stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(
                        ManagerInfoEntity::getUserId,
                        item -> item,
                        this::preferManagerInfo
                ));

        List<AdminClubManagerResponse> managers = new ArrayList<>();
        for (Long userId : managerUserIds) {
            UserEntity user = userMap.get(userId);
            if (user == null) {
                continue;
            }
            ManagerInfoEntity info = infoMap.get(userId);
            managers.add(new AdminClubManagerResponse(
                    user.getId(),
                    user.getUsername(),
                    info != null && info.getDisplayName() != null && !info.getDisplayName().isBlank() ? info.getDisplayName() : "",
                    info == null ? "" : defaultString(info.getManagerNo()),
                    info == null ? "" : defaultString(info.getPhone())
            ));
        }
        return managers;
    }

    private List<AdminClubStudentResponse> loadStudents(Long clubId) {
        List<ClubStudentMemberEntity> memberships = clubStudentMemberRepository.findAllByClubId(clubId);
        List<Long> studentUserIds = memberships.stream()
                .map(ClubStudentMemberEntity::getStudentUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (studentUserIds.isEmpty()) {
            return List.of();
        }
        Map<Long, ClubStudentMemberEntity> membershipMap = memberships.stream()
                .collect(Collectors.toMap(ClubStudentMemberEntity::getStudentUserId, item -> item, (left, right) -> left, HashMap::new));
        Map<Long, ClubDutyEntity> dutyMap = clubDutySupport.loadDutyMapByIds(
                memberships.stream().map(ClubStudentMemberEntity::getDutyId).filter(java.util.Objects::nonNull).distinct().toList()
        );

        List<UserEntity> users = userRepository.findAllByIdInAndRole(studentUserIds, UserRole.STUDENT);
        Map<Long, UserEntity> userMap = users.stream().collect(Collectors.toMap(UserEntity::getId, item -> item));
        Map<Long, StudentInfoEntity> infoMap = studentInfoRepository.findAllByUserIdIn(studentUserIds)
                .stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(
                        StudentInfoEntity::getUserId,
                        item -> item,
                        this::preferStudentInfo
                ));

        List<AdminClubStudentResponse> students = new ArrayList<>();
        for (Long userId : studentUserIds) {
            UserEntity user = userMap.get(userId);
            if (user == null) {
                continue;
            }
            StudentInfoEntity info = infoMap.get(userId);
            ClubStudentMemberEntity membership = membershipMap.get(userId);
            Long dutyId = membership == null ? null : membership.getDutyId();
            ClubDutyEntity duty = dutyId == null ? null : dutyMap.get(dutyId);
            students.add(new AdminClubStudentResponse(
                    user.getId(),
                    user.getUsername(),
                    info != null && info.getDisplayName() != null && !info.getDisplayName().isBlank() ? info.getDisplayName() : "",
                    info == null ? "" : defaultString(info.getStudentNo()),
                    info == null ? "" : defaultString(info.getGrade()),
                    info == null ? "" : defaultString(info.getClassName()),
                    duty == null ? null : duty.getId(),
                    duty == null ? "" : duty.getName(),
                    clubDutySupport.toPermissionValues(duty)
            ));
        }
        return students;
    }

    private Map<Long, Long> loadManagerCounts(Collection<Long> clubIds) {
        if (clubIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return clubManagerBindingRepository.countByClubIds(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }

    private Map<Long, Long> loadStudentCounts(Collection<Long> clubIds) {
        if (clubIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return clubStudentMemberRepository.countByClubIds(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }

    private void replaceManagerBindings(Long clubId, List<Long> managerUserIds) {
        clubManagerBindingRepository.deleteAllByClubId(clubId);
        if (managerUserIds.isEmpty()) {
            return;
        }

        List<ClubManagerBindingEntity> bindings = managerUserIds.stream()
                .map(userId -> {
                    ClubManagerBindingEntity entity = new ClubManagerBindingEntity();
                    entity.setClubId(clubId);
                    entity.setManagerUserId(userId);
                    return entity;
                })
                .toList();
        clubManagerBindingRepository.saveAll(bindings);
    }

    private boolean syncManagerBindings(Long clubId, List<ClubManagerBindingEntity> existingBindings, List<Long> managerUserIds) {
        Set<Long> existingManagerIds = existingBindings.stream()
                .map(ClubManagerBindingEntity::getManagerUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> requestedManagerIds = new LinkedHashSet<>(managerUserIds);

        Set<Long> managerIdsToRemove = new LinkedHashSet<>(existingManagerIds);
        managerIdsToRemove.removeAll(requestedManagerIds);

        Set<Long> managerIdsToAdd = new LinkedHashSet<>(requestedManagerIds);
        managerIdsToAdd.removeAll(existingManagerIds);

        if (!managerIdsToRemove.isEmpty()) {
            clubManagerBindingRepository.deleteAllByClubIdAndManagerUserIdIn(clubId, managerIdsToRemove);
        }

        if (!managerIdsToAdd.isEmpty()) {
            List<ClubManagerBindingEntity> bindingsToAdd = managerIdsToAdd.stream()
                    .map(userId -> {
                        ClubManagerBindingEntity entity = new ClubManagerBindingEntity();
                        entity.setClubId(clubId);
                        entity.setManagerUserId(userId);
                        return entity;
                    })
                    .toList();
            clubManagerBindingRepository.saveAll(bindingsToAdd);
        }

        return !existingManagerIds.equals(requestedManagerIds);
    }

    private void validateManagerUsers(List<Long> managerUserIds) {
        if (managerUserIds.isEmpty()) {
            return;
        }

        List<UserEntity> managerUsers = userRepository.findAllByIdInAndRole(managerUserIds, UserRole.CLUB_MANAGER);
        if (managerUsers.size() != managerUserIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid manager users");
        }
    }

    private List<Long> normalizeManagerUserIds(List<Long> managerUserIds) {
        if (managerUserIds == null || managerUserIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Long value : managerUserIds) {
            if (value == null || value <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid manager user ids");
            }
            ids.add(value);
        }
        return List.copyOf(ids);
    }

    private ClubStatus parseStatus(String status, boolean allowBlank) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            if (allowBlank) {
                return null;
            }
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club status is required");
        }

        try {
            return ClubStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid club status");
        }
    }

    private String normalizeName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club name is required");
        }
        if (normalized.length() > MAX_CLUB_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club name too long");
        }
        return normalized;
    }

    private String normalizeType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club type is required");
        }
        return normalized;
    }

    private String normalizeKeyword(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (!user.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return user;
    }

    private AdminManagerOptionResponse toManagerOption(UserEntity user, ManagerInfoEntity info) {
        String displayName = info != null && info.getDisplayName() != null && !info.getDisplayName().isBlank()
                ? info.getDisplayName()
                : "";
        return new AdminManagerOptionResponse(
                user.getId(),
                user.getUsername(),
                displayName,
                info == null ? "" : defaultString(info.getManagerNo())
        );
    }

    private boolean matchesKeyword(AdminManagerOptionResponse option, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }
        return buildManagerOptionSearchText(option).contains(keyword);
    }

    private String buildManagerOptionSearchText(AdminManagerOptionResponse option) {
        return String.join(
                " ",
                option.username().toLowerCase(Locale.ROOT),
                option.displayName().toLowerCase(Locale.ROOT),
                option.managerNo().toLowerCase(Locale.ROOT)
        );
    }

    private String buildManagerOptionSortKey(AdminManagerOptionResponse option) {
        return option.displayName() + " " + option.username();
    }

    private StudentInfoEntity preferStudentInfo(StudentInfoEntity left, StudentInfoEntity right) {
        return Comparator
                .comparing((StudentInfoEntity item) -> item.getUpdatedAt(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> item.getId(), Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(left, right) >= 0 ? left : right;
    }

    private ManagerInfoEntity preferManagerInfo(ManagerInfoEntity left, ManagerInfoEntity right) {
        return Comparator
                .comparing((ManagerInfoEntity item) -> item.getUpdatedAt(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> item.getId(), Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(left, right) >= 0 ? left : right;
    }
}
