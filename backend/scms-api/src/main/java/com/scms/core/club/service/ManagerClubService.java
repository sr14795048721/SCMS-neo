package com.scms.core.club.service;

import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubJoinRequestEntity;
import com.scms.core.club.domain.ClubJoinRequestStatus;
import com.scms.core.club.domain.ClubManagerBindingEntity;
import com.scms.core.club.domain.ClubMemberRole;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.ManagerClubDetailResponse;
import com.scms.core.club.dto.ManagerClubJoinRequestResponse;
import com.scms.core.club.dto.ManagerClubMemberResponse;
import com.scms.core.club.dto.ManagerClubMemberRoleUpdateRequest;
import com.scms.core.club.dto.ManagerClubSummaryResponse;
import com.scms.core.club.dto.StudentClubJoinRequestCreateRequest;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRelationCountProjection;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ManagerClubService {

    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final ManagerInfoRepository managerInfoRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ClubDutySupport clubDutySupport;

    public ManagerClubService(ClubRepository clubRepository,
                              ClubManagerBindingRepository clubManagerBindingRepository,
                              ClubStudentMemberRepository clubStudentMemberRepository,
                              ClubJoinRequestRepository clubJoinRequestRepository,
                              ActivityRepository activityRepository,
                              UserRepository userRepository,
                              StudentInfoRepository studentInfoRepository,
                              ManagerInfoRepository managerInfoRepository,
                              CurrentUserProvider currentUserProvider,
                              AuditService auditService,
                              NotificationService notificationService,
                              ClubDutySupport clubDutySupport) {
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.clubJoinRequestRepository = clubJoinRequestRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.managerInfoRepository = managerInfoRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.clubDutySupport = clubDutySupport;
    }

    @Transactional(readOnly = true)
    public List<ManagerClubSummaryResponse> listManagedClubs() {
        AuthenticatedUser manager = requireManager();
        List<Long> clubIds = clubManagerBindingRepository.findAllByManagerUserId(manager.userId())
                .stream()
                .map(ClubManagerBindingEntity::getClubId)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ClubEntity> clubMap = loadClubMap(clubIds);
        Map<Long, Long> memberCounts = loadMemberCounts(clubIds);
        Map<Long, Long> pendingJoinRequestCounts = loadJoinRequestCounts(clubIds, ClubJoinRequestStatus.PENDING);
        Map<Long, Long> draftActivityCounts = loadActivityCounts(clubIds, ActivityStatus.DRAFT);

        return clubIds.stream()
                .map(clubMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ClubEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(club -> new ManagerClubSummaryResponse(
                        club.getId(),
                        club.getName(),
                        defaultString(club.getType()),
                        defaultString(club.getDescription()),
                        memberCounts.getOrDefault(club.getId(), 0L),
                        pendingJoinRequestCounts.getOrDefault(club.getId(), 0L),
                        draftActivityCounts.getOrDefault(club.getId(), 0L),
                        club.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ManagerClubDetailResponse getManagedClubDetail(Long clubId) {
        requireManagerAccessToClub(clubId);
        ClubEntity club = getRequiredClub(clubId);

        return new ManagerClubDetailResponse(
                club.getId(),
                club.getName(),
                defaultString(club.getType()),
                defaultString(club.getDescription()),
                club.getStatus().name(),
                clubStudentMemberRepository.findAllByClubId(clubId).size(),
                clubJoinRequestRepository.countByClubIdAndStatus(clubId, ClubJoinRequestStatus.PENDING),
                clubJoinRequestRepository.findAllByClubIdOrderByCreatedAtDescIdDesc(clubId).size(),
                activityRepository.findAllByClubIdInOrderByCreatedAtDescIdDesc(List.of(clubId)).size(),
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ManagerClubMemberResponse> listMembers(Long clubId) {
        requireManagerAccessToClub(clubId);

        List<ClubStudentMemberEntity> members = clubStudentMemberRepository.findAllByClubId(clubId);
        List<Long> studentIds = members.stream().map(ClubStudentMemberEntity::getStudentUserId).distinct().toList();
        Map<Long, UserEntity> userMap = loadStudentUserMap(studentIds);
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(studentIds);
        Map<Long, ClubDutyEntity> dutyMap = clubDutySupport.loadDutyMapByIds(
                members.stream().map(ClubStudentMemberEntity::getDutyId).filter(Objects::nonNull).distinct().toList()
        );

        return members.stream()
                .sorted(Comparator
                        .comparing(
                                (ClubStudentMemberEntity item) -> {
                                    Long dutyId = item.getDutyId();
                                    ClubDutyEntity duty = dutyId == null ? null : dutyMap.get(dutyId);
                                    return duty == null ? null : trimToNull(duty.getName());
                                },
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparing(
                                item -> trimToNull(resolveStudentDisplayName(item.getStudentUserId(), userMap, studentInfoMap)),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparing(ClubStudentMemberEntity::getStudentUserId))
                .map(member -> toMemberResponse(member, userMap, studentInfoMap, dutyMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagerClubJoinRequestResponse> listJoinRequests(Long clubId) {
        requireManagerAccessToClub(clubId);

        List<ClubJoinRequestEntity> requests = clubJoinRequestRepository.findAllByClubIdOrderByCreatedAtDescIdDesc(clubId);
        List<Long> studentIds = requests.stream().map(ClubJoinRequestEntity::getStudentUserId).distinct().toList();
        List<Long> reviewerIds = requests.stream().map(ClubJoinRequestEntity::getReviewedBy).filter(Objects::nonNull).distinct().toList();

        Map<Long, UserEntity> studentMap = loadStudentUserMap(studentIds);
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(studentIds);
        Map<Long, UserEntity> reviewerMap = loadUserMap(reviewerIds);
        Map<Long, ManagerInfoEntity> reviewerInfoMap = loadManagerInfoMap(reviewerIds);

        return requests.stream()
                .map(request -> toJoinRequestResponse(request, studentMap, studentInfoMap, reviewerMap, reviewerInfoMap))
                .toList();
    }

    @Transactional
    public ManagerClubJoinRequestResponse approveJoinRequest(Long clubId, Long requestId) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        ClubJoinRequestEntity request = getRequiredJoinRequest(clubId, requestId);
        lockRequiredStudentUser(request.getStudentUserId());

        if (request.getStatus() == ClubJoinRequestStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "rejected join request cannot be approved");
        }

        boolean belongsToAnotherClub = clubStudentMemberRepository.findAllByStudentUserId(request.getStudentUserId())
                .stream()
                .anyMatch(member -> !Objects.equals(member.getClubId(), clubId));
        if (belongsToAnotherClub) {
            throw new BusinessException(ErrorCode.CONFLICT, "student is already a member of another club");
        }

        if (!clubStudentMemberRepository.existsByClubIdAndStudentUserId(clubId, request.getStudentUserId())) {
            ClubStudentMemberEntity member = new ClubStudentMemberEntity();
            member.setClubId(clubId);
            member.setStudentUserId(request.getStudentUserId());
            member.setRole(ClubMemberRole.MEMBER);
            member.setDutyId(null);
            saveMemberWithConflictTranslation(member);
        }

        if (request.getStatus() != ClubJoinRequestStatus.APPROVED) {
            request.setStatus(ClubJoinRequestStatus.APPROVED);
            request.setReviewedBy(manager.userId());
            request.setReviewedAt(Instant.now());
            request = clubJoinRequestRepository.save(request);
            auditService.create("MANAGER_CLUB_JOIN_REQUEST_APPROVED", manager.userId(), "CLUB_JOIN_REQUEST", String.valueOf(request.getId()), "Club manager approved a join request");
        }

        return buildJoinRequestResponse(request);
    }

    @Transactional
    public ManagerClubJoinRequestResponse rejectJoinRequest(Long clubId, Long requestId) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        ClubJoinRequestEntity request = getRequiredJoinRequest(clubId, requestId);

        if (request.getStatus() == ClubJoinRequestStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "approved join request cannot be rejected");
        }

        if (request.getStatus() != ClubJoinRequestStatus.REJECTED) {
            request.setStatus(ClubJoinRequestStatus.REJECTED);
            request.setReviewedBy(manager.userId());
            request.setReviewedAt(Instant.now());
            request = clubJoinRequestRepository.save(request);
            auditService.create("MANAGER_CLUB_JOIN_REQUEST_REJECTED", manager.userId(), "CLUB_JOIN_REQUEST", String.valueOf(request.getId()), "Club manager rejected a join request");
        }

        return buildJoinRequestResponse(request);
    }

    @Transactional
    public ManagerClubMemberResponse updateMemberRole(Long clubId, Long studentUserId, ManagerClubMemberRoleUpdateRequest request) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        ClubStudentMemberEntity member = clubStudentMemberRepository.findByClubIdAndStudentUserId(clubId, studentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club member not found"));

        member.setRole(parseMemberRole(request.role()));
        ClubStudentMemberEntity saved = clubStudentMemberRepository.save(member);
        auditService.create("MANAGER_CLUB_MEMBER_ROLE_UPDATED", manager.userId(), "CLUB_MEMBER", String.valueOf(saved.getId()), "Club manager updated a member role");
        return toMemberResponse(
                saved,
                loadStudentUserMap(List.of(studentUserId)),
                loadStudentInfoMap(List.of(studentUserId)),
                clubDutySupport.loadDutyMapByIds(saved.getDutyId() == null ? List.of() : List.of(saved.getDutyId()))
        );
    }

    @Transactional
    public void removeMember(Long clubId, Long studentUserId) {
        AuthenticatedUser manager = requireManagerAccessToClub(clubId);
        ClubStudentMemberEntity member = clubStudentMemberRepository.findByClubIdAndStudentUserId(clubId, studentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club member not found"));

        clubStudentMemberRepository.delete(member);
        auditService.create(
                "MANAGER_CLUB_MEMBER_REMOVED",
                manager.userId(),
                "CLUB_MEMBER",
                String.valueOf(studentUserId),
                "Club manager removed a club member"
        );
    }

    @Transactional
    public ManagerClubJoinRequestResponse createStudentJoinRequest(StudentClubJoinRequestCreateRequest request) {
        AuthenticatedUser student = requireStudent();
        ClubEntity club = getRequiredClub(request.clubId());
        if (club.getStatus() != ClubStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "club is not active");
        }
        lockRequiredStudentUser(student.userId());
        if (clubStudentMemberRepository.existsByStudentUserId(student.userId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "student is already a club member");
        }
        if (clubJoinRequestRepository.existsByStudentUserIdAndStatus(student.userId(), ClubJoinRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CONFLICT, "join request is already pending");
        }

        ClubJoinRequestEntity entity = new ClubJoinRequestEntity();
        entity.setClubId(club.getId());
        entity.setStudentUserId(student.userId());
        entity.setReason(trimToNull(request.reason()));
        entity.setStatus(ClubJoinRequestStatus.PENDING);

        ClubJoinRequestEntity saved = saveJoinRequestWithConflictTranslation(entity);
        auditService.create("STUDENT_CLUB_JOIN_REQUEST_CREATED", student.userId(), "CLUB_JOIN_REQUEST", String.valueOf(saved.getId()), "Student created a club join request");
        notifyManagersForJoinRequest(club.getId(), club.getName());
        return buildJoinRequestResponse(saved);
    }

    private void notifyManagersForJoinRequest(Long clubId, String clubName) {
        String message = "A student submitted a new join request for " + defaultString(clubName) + ".";
        List<Long> managerUserIds = clubManagerBindingRepository.findAllByClubId(clubId)
                .stream()
                .map(ClubManagerBindingEntity::getManagerUserId)
                .distinct()
                .toList();
        for (Long managerUserId : managerUserIds) {
            if (managerUserId == null) {
                continue;
            }
            notificationService.create(
                    managerUserId,
                    "CLUB_JOIN_REQUEST",
                    "new join request",
                    message,
                    "/club-admin/clubs/" + clubId
            );
        }
    }

    private ManagerClubMemberResponse toMemberResponse(ClubStudentMemberEntity member,
                                                       Map<Long, UserEntity> userMap,
                                                       Map<Long, StudentInfoEntity> studentInfoMap,
                                                       Map<Long, ClubDutyEntity> dutyMap) {
        UserEntity user = userMap.get(member.getStudentUserId());
        StudentInfoEntity info = studentInfoMap.get(member.getStudentUserId());
        Long dutyId = member.getDutyId();
        ClubDutyEntity duty = dutyId == null ? null : dutyMap.get(dutyId);
        return new ManagerClubMemberResponse(
                member.getStudentUserId(),
                user == null ? "" : user.getUsername(),
                resolveStudentDisplayName(member.getStudentUserId(), userMap, studentInfoMap),
                info == null ? "" : defaultString(info.getStudentNo()),
                info == null ? "" : defaultString(info.getGrade()),
                info == null ? "" : defaultString(info.getClassName()),
                member.getRole() == null ? ClubMemberRole.MEMBER.name() : member.getRole().name(),
                duty == null ? null : duty.getId(),
                duty == null ? "" : duty.getName(),
                clubDutySupport.toPermissionValues(duty),
                member.getCreatedAt()
        );
    }

    private ManagerClubJoinRequestResponse buildJoinRequestResponse(ClubJoinRequestEntity request) {
        Map<Long, UserEntity> studentMap = loadStudentUserMap(List.of(request.getStudentUserId()));
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(List.of(request.getStudentUserId()));
        Map<Long, UserEntity> reviewerMap = request.getReviewedBy() == null ? Map.of() : loadUserMap(List.of(request.getReviewedBy()));
        Map<Long, ManagerInfoEntity> reviewerInfoMap = request.getReviewedBy() == null ? Map.of() : loadManagerInfoMap(List.of(request.getReviewedBy()));
        return toJoinRequestResponse(request, studentMap, studentInfoMap, reviewerMap, reviewerInfoMap);
    }

    private ManagerClubJoinRequestResponse toJoinRequestResponse(ClubJoinRequestEntity request,
                                                                 Map<Long, UserEntity> studentMap,
                                                                 Map<Long, StudentInfoEntity> studentInfoMap,
                                                                 Map<Long, UserEntity> reviewerMap,
                                                                 Map<Long, ManagerInfoEntity> reviewerInfoMap) {
        StudentInfoEntity studentInfo = studentInfoMap.get(request.getStudentUserId());
        return new ManagerClubJoinRequestResponse(
                request.getId(),
                request.getClubId(),
                request.getStudentUserId(),
                studentMap.get(request.getStudentUserId()) == null ? "" : studentMap.get(request.getStudentUserId()).getUsername(),
                resolveStudentDisplayName(request.getStudentUserId(), studentMap, studentInfoMap),
                studentInfo == null ? "" : defaultString(studentInfo.getStudentNo()),
                studentInfo == null ? "" : defaultString(studentInfo.getGrade()),
                studentInfo == null ? "" : defaultString(studentInfo.getClassName()),
                defaultString(request.getReason()),
                request.getStatus().name(),
                request.getReviewedBy(),
                resolveManagerDisplayName(request.getReviewedBy(), reviewerMap, reviewerInfoMap),
                request.getReviewedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private ClubJoinRequestEntity getRequiredJoinRequest(Long clubId, Long requestId) {
        return clubJoinRequestRepository.findByIdAndClubId(requestId, clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club join request not found"));
    }

    private UserEntity lockRequiredStudentUser(Long studentUserId) {
        UserEntity student = userRepository.findByIdForUpdate(studentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "student not found"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "student not found");
        }
        return student;
    }

    private void saveMemberWithConflictTranslation(ClubStudentMemberEntity member) {
        try {
            clubStudentMemberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "student is already a member of another club");
        }
    }

    private ClubJoinRequestEntity saveJoinRequestWithConflictTranslation(ClubJoinRequestEntity entity) {
        try {
            return clubJoinRequestRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "join request is already pending");
        }
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private AuthenticatedUser requireManager() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager role required");
        }
        return user;
    }

    private AuthenticatedUser requireManagerAccessToClub(Long clubId) {
        AuthenticatedUser manager = requireManager();
        if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, manager.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager permission denied");
        }
        return manager;
    }

    private AuthenticatedUser requireStudent() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return user;
    }

    private ClubMemberRole parseMemberRole(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "member role is required");
        }
        try {
            return ClubMemberRole.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "member role is invalid");
        }
    }

    private Map<Long, ClubEntity> loadClubMap(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        return clubRepository.findAllById(clubIds).stream().collect(Collectors.toMap(ClubEntity::getId, item -> item));
    }

    private Map<Long, Long> loadMemberCounts(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        return clubStudentMemberRepository.countByClubIds(clubIds).stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }

    private Map<Long, Long> loadJoinRequestCounts(Collection<Long> clubIds, ClubJoinRequestStatus status) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        return clubJoinRequestRepository.countByClubIdsAndStatus(clubIds, status).stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }

    private Map<Long, Long> loadActivityCounts(Collection<Long> clubIds, ActivityStatus status) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        return activityRepository.countByClubIdsAndStatus(clubIds, status).stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }

    private Map<Long, UserEntity> loadStudentUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIdInAndRole(normalizedIds, UserRole.STUDENT).stream()
                .collect(Collectors.toMap(UserEntity::getId, item -> item));
    }

    private Map<Long, UserEntity> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserEntity> result = new LinkedHashMap<>();
        for (UserEntity user : userRepository.findAllById(normalizedIds)) {
            result.put(user.getId(), user);
        }
        return result;
    }

    private Map<Long, StudentInfoEntity> loadStudentInfoMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }
        return studentInfoRepository.findAllByUserIdIn(normalizedIds).stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(
                        StudentInfoEntity::getUserId,
                        item -> item,
                        this::preferStudentInfo,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, ManagerInfoEntity> loadManagerInfoMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }
        return managerInfoRepository.findAllByUserIdIn(normalizedIds).stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(
                        ManagerInfoEntity::getUserId,
                        item -> item,
                        this::preferManagerInfo,
                        LinkedHashMap::new
                ));
    }

    private String resolveStudentDisplayName(Long userId, Map<Long, UserEntity> userMap, Map<Long, StudentInfoEntity> studentInfoMap) {
        StudentInfoEntity info = studentInfoMap.get(userId);
        if (info != null && trimToNull(info.getDisplayName()) != null) {
            return info.getDisplayName();
        }
        UserEntity user = userMap.get(userId);
        return user == null ? "" : user.getUsername();
    }

    private String resolveManagerDisplayName(Long userId, Map<Long, UserEntity> userMap, Map<Long, ManagerInfoEntity> managerInfoMap) {
        if (userId == null) {
            return "";
        }
        ManagerInfoEntity info = managerInfoMap.get(userId);
        if (info != null && trimToNull(info.getDisplayName()) != null) {
            return info.getDisplayName();
        }
        UserEntity user = userMap.get(userId);
        return user == null ? "" : user.getUsername();
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
