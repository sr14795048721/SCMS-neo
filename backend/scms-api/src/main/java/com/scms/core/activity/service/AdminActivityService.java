package com.scms.core.activity.service;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.activity.dto.AdminActivityDetailResponse;
import com.scms.core.activity.dto.AdminActivityListItemResponse;
import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.dto.AdminActivityRegistrationResponse;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.adminuser.dto.AdminUserPageResponse;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.event.DomainEvent;
import com.scms.core.event.DomainEventPublisher;
import com.scms.core.event.DomainEventTopics;
import com.scms.core.registration.domain.RegistrationEntity;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.registration.repository.ActivityRegistrationCountProjection;
import com.scms.core.registration.repository.RegistrationRepository;
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

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminActivityService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ActivityRepository activityRepository;
    private final ClubRepository clubRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final DomainEventPublisher domainEventPublisher;

    public AdminActivityService(ActivityRepository activityRepository,
                                ClubRepository clubRepository,
                                RegistrationRepository registrationRepository,
                                UserRepository userRepository,
                                StudentInfoRepository studentInfoRepository,
                                CurrentUserProvider currentUserProvider,
                                AuditService auditService,
                                DomainEventPublisher domainEventPublisher) {
        this.activityRepository = activityRepository;
        this.clubRepository = clubRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse<AdminActivityListItemResponse> list(int page,
                                                                     int pageSize,
                                                                     String keyword,
                                                                     Long clubId,
                                                                     String status) {
        requireAdmin();

        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        String keywordLike = normalizedKeyword.isEmpty() ? "%" : "%" + normalizedKeyword + "%";
        ActivityStatus activityStatus = parseStatus(status, true);

        Page<ActivityEntity> activities = activityRepository.searchAdmin(
                normalizedKeyword,
                keywordLike,
                clubId,
                activityStatus,
                PageRequest.of(normalizedPage - 1, normalizedPageSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );

        List<ActivityEntity> items = activities.getContent();
        Map<Long, ClubEntity> clubMap = loadClubMap(items.stream().map(ActivityEntity::getClubId).toList());
        Map<Long, Long> registrationCounts = loadRegistrationCounts(items.stream().map(ActivityEntity::getId).toList());

        return new AdminUserPageResponse<>(
                items.stream()
                        .map((activity) -> toListItem(activity, clubMap.get(activity.getClubId()), registrationCounts.getOrDefault(activity.getId(), 0L)))
                        .toList(),
                normalizedPage,
                normalizedPageSize,
                activities.getTotalElements(),
                activities.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminActivityDetailResponse getDetail(Long activityId) {
        requireAdmin();
        return buildDetail(getRequiredActivity(activityId));
    }

    @Transactional
    public AdminActivityDetailResponse create(AdminActivityMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        ClubEntity club = getRequiredActiveClub(request.clubId());
        validateMutationRequest(request, 0L, false);

        ActivityEntity entity = new ActivityEntity();
        entity.setClubId(club.getId());
        entity.setTitle(normalizeTitle(request.title()));
        entity.setDescription(trimToNull(request.description()));
        entity.setLocation(trimToNull(request.location()));
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setCapacity(request.capacity());
        entity.setStatus(ActivityStatus.DRAFT);
        entity.setCreatedBy(admin.userId());

        ActivityEntity saved = activityRepository.save(entity);
        auditService.create("ADMIN_ACTIVITY_CREATED", admin.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Administrator created an activity");
        return buildDetail(saved);
    }

    @Transactional
    public AdminActivityDetailResponse update(Long activityId, AdminActivityMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        ActivityEntity activity = getRequiredActivity(activityId);
        ClubEntity club = getRequiredActiveClub(request.clubId());
        long registeredCount = loadRegisteredCount(activityId);
        validateMutationRequest(request, registeredCount, activity.getStatus() == ActivityStatus.PUBLISHED);

        activity.setClubId(club.getId());
        activity.setTitle(normalizeTitle(request.title()));
        activity.setDescription(trimToNull(request.description()));
        activity.setLocation(trimToNull(request.location()));
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setCapacity(request.capacity());

        ActivityEntity saved = activityRepository.save(activity);
        auditService.create("ADMIN_ACTIVITY_UPDATED", admin.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Administrator updated an activity");
        return buildDetail(saved);
    }

    @Transactional
    public AdminActivityDetailResponse publish(Long activityId) {
        AuthenticatedUser admin = requireAdmin();
        ActivityEntity activity = getRequiredActivity(activityId);
        getRequiredActiveClub(activity.getClubId());

        if (activity.getStatus() == ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity already published");
        }
        if (activity.getStatus() == ActivityStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "closed activity cannot be published");
        }

        activity.setStatus(ActivityStatus.PUBLISHED);
        ActivityEntity saved = activityRepository.save(activity);
        publishActivityEvent(saved, admin.userId());
        auditService.create("ADMIN_ACTIVITY_PUBLISHED", admin.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Administrator published an activity");
        return buildDetail(saved);
    }

    @Transactional
    public AdminActivityDetailResponse close(Long activityId) {
        AuthenticatedUser admin = requireAdmin();
        ActivityEntity activity = getRequiredActivity(activityId);

        if (activity.getStatus() == ActivityStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity already closed");
        }
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "only published activity can be closed");
        }

        activity.setStatus(ActivityStatus.CLOSED);
        ActivityEntity saved = activityRepository.save(activity);
        auditService.create("ADMIN_ACTIVITY_CLOSED", admin.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Administrator closed an activity");
        return buildDetail(saved);
    }

    @Transactional
    public void delete(Long activityId) {
        AuthenticatedUser admin = requireAdmin();
        ActivityEntity activity = getRequiredActivity(activityId);

        if (registrationRepository.countByActivityId(activityId) > 0) {
            throw new BusinessException(ErrorCode.DEPENDENCY_EXISTS, "activity still has registrations");
        }

        activityRepository.delete(activity);
        auditService.create("ADMIN_ACTIVITY_DELETED", admin.userId(), "ACTIVITY", String.valueOf(activityId), "Administrator deleted an activity");
    }

    private AdminActivityDetailResponse buildDetail(ActivityEntity activity) {
        ClubEntity club = clubRepository.findById(activity.getClubId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));

        List<RegistrationEntity> registrations = registrationRepository.findByActivityIdAndStatusOrderByCreatedAtDesc(
                activity.getId(),
                RegistrationStatus.REGISTERED
        );
        List<Long> registeredUserIds = registrations.stream().map(RegistrationEntity::getUserId).distinct().toList();
        Map<Long, UserEntity> userMap = loadUserMap(registeredUserIds);
        Map<Long, StudentInfoEntity> studentMap = loadStudentInfoMap(registeredUserIds);

        return new AdminActivityDetailResponse(
                activity.getId(),
                activity.getClubId(),
                club.getName(),
                activity.getTitle(),
                defaultString(activity.getDescription()),
                defaultString(activity.getLocation()),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getCapacity(),
                activity.getStatus().name(),
                registrations.size(),
                activity.getCreatedAt(),
                activity.getUpdatedAt(),
                registrations.stream()
                        .map((registration) -> toRegistrationResponse(registration, userMap.get(registration.getUserId()), studentMap.get(registration.getUserId())))
                        .toList()
        );
    }

    private AdminActivityListItemResponse toListItem(ActivityEntity activity, ClubEntity club, long registrationCount) {
        return new AdminActivityListItemResponse(
                activity.getId(),
                activity.getClubId(),
                club != null ? club.getName() : "",
                activity.getTitle(),
                defaultString(activity.getDescription()),
                defaultString(activity.getLocation()),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getCapacity(),
                activity.getStatus().name(),
                registrationCount,
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }

    private AdminActivityRegistrationResponse toRegistrationResponse(RegistrationEntity registration,
                                                                     UserEntity user,
                                                                     StudentInfoEntity studentInfo) {
        return new AdminActivityRegistrationResponse(
                registration.getId(),
                registration.getUserId(),
                user != null ? user.getUsername() : "",
                studentInfo != null ? defaultString(studentInfo.getDisplayName()) : "",
                studentInfo != null ? defaultString(studentInfo.getStudentNo()) : "",
                studentInfo != null ? defaultString(studentInfo.getGrade()) : "",
                studentInfo != null ? defaultString(studentInfo.getClassName()) : "",
                registration.getCreatedAt()
        );
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (!user.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin permission required");
        }
        return user;
    }

    private ActivityEntity getRequiredActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "activity not found"));
    }

    private ClubEntity getRequiredActiveClub(Long clubId) {
        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
        if (club.getStatus() != ClubStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "club is not active");
        }
        return club;
    }

    private void validateMutationRequest(AdminActivityMutationRequest request, long registeredCount, boolean published) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "endTime must be after startTime");
        }
        if (request.capacity() < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "capacity must be greater than 0");
        }
        if (published && request.capacity() < registeredCount) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "capacity cannot be less than current registrations");
        }
    }

    private ActivityStatus parseStatus(String status, boolean allowBlank) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            if (allowBlank) {
                return null;
            }
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "activity status is required");
        }
        try {
            return ActivityStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "activity status is invalid");
        }
    }

    private String normalizeKeyword(String keyword) {
        return String.valueOf(keyword == null ? "" : keyword).trim().toLowerCase(Locale.ROOT);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        int normalized = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        return Math.min(normalized, MAX_PAGE_SIZE);
    }

    private String normalizeTitle(String title) {
        String normalized = trimToNull(title);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title is required");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long loadRegisteredCount(Long activityId) {
        return registrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.REGISTERED);
    }

    private Map<Long, Long> loadRegistrationCounts(Collection<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        return registrationRepository.countByActivityIdsAndStatus(activityIds, RegistrationStatus.REGISTERED)
                .stream()
                .collect(Collectors.toMap(ActivityRegistrationCountProjection::getActivityId, ActivityRegistrationCountProjection::getRegistrationCount));
    }

    private Map<Long, ClubEntity> loadClubMap(Collection<Long> clubIds) {
        if (clubIds.isEmpty()) {
            return Map.of();
        }
        return clubRepository.findAllById(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubEntity::getId, item -> item));
    }

    private Map<Long, UserEntity> loadUserMap(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserEntity> userMap = new LinkedHashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    private Map<Long, StudentInfoEntity> loadStudentInfoMap(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return studentInfoRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(StudentInfoEntity::getUserId, item -> item));
    }

    private void publishActivityEvent(ActivityEntity activity, Long operatorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityId", activity.getId());
        payload.put("clubId", activity.getClubId());
        payload.put("operatorId", operatorId);
        payload.put("title", activity.getTitle());
        domainEventPublisher.publish(new DomainEvent(
                DomainEventTopics.ACTIVITY_PUBLISHED,
                String.valueOf(activity.getId()),
                Instant.now(),
                payload
        ));
    }

    private String defaultString(String value) {
        return Objects.toString(value, "");
    }
}
