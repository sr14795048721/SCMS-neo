package com.scms.core.activity.service;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.dto.ManagerActivityResponse;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.event.DomainEvent;
import com.scms.core.event.DomainEventPublisher;
import com.scms.core.event.DomainEventTopics;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.registration.repository.ActivityRegistrationCountProjection;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ManagerActivityService {

    private final ActivityRepository activityRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final RegistrationRepository registrationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final DomainEventPublisher domainEventPublisher;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ManagerActivityService(ActivityRepository activityRepository,
                                  ClubRepository clubRepository,
                                  ClubManagerBindingRepository clubManagerBindingRepository,
                                  RegistrationRepository registrationRepository,
                                  CurrentUserProvider currentUserProvider,
                                  AuditService auditService,
                                  DomainEventPublisher domainEventPublisher,
                                  UserRepository userRepository,
                                  NotificationService notificationService) {
        this.activityRepository = activityRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.registrationRepository = registrationRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.domainEventPublisher = domainEventPublisher;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ManagerActivityResponse> listManagedActivities() {
        AuthenticatedUser manager = requireManager();
        List<Long> clubIds = clubManagerBindingRepository.findAllByManagerUserId(manager.userId())
                .stream()
                .map(item -> item.getClubId())
                .distinct()
                .toList();
        if (clubIds.isEmpty()) {
            return List.of();
        }

        List<ActivityEntity> activities = activityRepository.findAllByClubIdInOrderByCreatedAtDescIdDesc(clubIds);
        Map<Long, ClubEntity> clubMap = loadClubMap(clubIds);
        Map<Long, Long> registrationCounts = loadRegistrationCounts(activities.stream().map(ActivityEntity::getId).toList());

        return activities.stream()
                .sorted(Comparator.comparing(ActivityEntity::getCreatedAt).reversed().thenComparing(ActivityEntity::getId, Comparator.reverseOrder()))
                .map(activity -> toResponse(activity, clubMap.get(activity.getClubId()), registrationCounts.getOrDefault(activity.getId(), 0L)))
                .toList();
    }

    @Transactional
    public ManagerActivityResponse create(AdminActivityMutationRequest request) {
        AuthenticatedUser manager = requireManager();
        ClubEntity club = requireManagedActiveClub(request.clubId(), manager.userId());
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
        entity.setCreatedBy(manager.userId());

        ActivityEntity saved = activityRepository.save(entity);
        auditService.create("MANAGER_ACTIVITY_CREATED", manager.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Club manager created an activity");
        return toResponse(saved, club, 0L);
    }

    @Transactional
    public ManagerActivityResponse update(Long activityId, AdminActivityMutationRequest request) {
        AuthenticatedUser manager = requireManager();
        ActivityEntity activity = getRequiredManagedActivity(activityId, manager.userId());
        ClubEntity club = requireManagedActiveClub(request.clubId(), manager.userId());
        long registeredCount = registrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.REGISTERED);
        validateMutationRequest(request, registeredCount, activity.getStatus() == ActivityStatus.PUBLISHED);

        activity.setClubId(club.getId());
        activity.setTitle(normalizeTitle(request.title()));
        activity.setDescription(trimToNull(request.description()));
        activity.setLocation(trimToNull(request.location()));
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setCapacity(request.capacity());

        ActivityEntity saved = activityRepository.save(activity);
        auditService.create("MANAGER_ACTIVITY_UPDATED", manager.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Club manager updated an activity");
        return toResponse(saved, club, registeredCount);
    }

    @Transactional
    public ManagerActivityResponse publish(Long activityId) {
        AuthenticatedUser manager = requireManager();
        ActivityEntity activity = getRequiredManagedActivity(activityId, manager.userId());
        ClubEntity club = requireManagedActiveClub(activity.getClubId(), manager.userId());

        if (activity.getStatus() == ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity already published");
        }
        if (activity.getStatus() == ActivityStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "closed activity cannot be published");
        }

        activity.setStatus(ActivityStatus.PUBLISHED);
        ActivityEntity saved = activityRepository.save(activity);
        notifyStudentsActivityPublished(saved, club);
        publishActivityEvent(saved, manager.userId());
        auditService.create("MANAGER_ACTIVITY_PUBLISHED", manager.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Club manager published an activity");
        return toResponse(saved, club, registrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.REGISTERED));
    }

    @Transactional
    public ManagerActivityResponse close(Long activityId) {
        AuthenticatedUser manager = requireManager();
        ActivityEntity activity = getRequiredManagedActivity(activityId, manager.userId());
        ClubEntity club = requireManagedActiveClub(activity.getClubId(), manager.userId());

        if (activity.getStatus() == ActivityStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity already closed");
        }
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "only published activity can be closed");
        }

        activity.setStatus(ActivityStatus.CLOSED);
        ActivityEntity saved = activityRepository.save(activity);
        auditService.create("MANAGER_ACTIVITY_CLOSED", manager.userId(), "ACTIVITY", String.valueOf(saved.getId()), "Club manager closed an activity");
        return toResponse(saved, club, registrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.REGISTERED));
    }

    private ActivityEntity getRequiredManagedActivity(Long activityId, Long managerUserId) {
        ActivityEntity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "activity not found"));
        if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(activity.getClubId(), managerUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager cannot access this activity");
        }
        return activity;
    }

    private ClubEntity requireManagedActiveClub(Long clubId, Long managerUserId) {
        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
        if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, managerUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager cannot access this club");
        }
        if (club.getStatus() != ClubStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "club is not active");
        }
        return club;
    }

    private AuthenticatedUser requireManager() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager role required");
        }
        return user;
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
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Map<Long, ClubEntity> loadClubMap(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        return clubRepository.findAllById(clubIds).stream().collect(Collectors.toMap(ClubEntity::getId, item -> item));
    }

    private Map<Long, Long> loadRegistrationCounts(Collection<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }
        return registrationRepository.countByActivityIdsAndStatus(activityIds, RegistrationStatus.REGISTERED).stream()
                .collect(Collectors.toMap(ActivityRegistrationCountProjection::getActivityId, ActivityRegistrationCountProjection::getRegistrationCount));
    }

    private ManagerActivityResponse toResponse(ActivityEntity activity, ClubEntity club, long registrationCount) {
        return new ManagerActivityResponse(
                activity.getId(),
                activity.getClubId(),
                club == null ? "" : club.getName(),
                activity.getTitle(),
                Objects.toString(activity.getDescription(), ""),
                Objects.toString(activity.getLocation(), ""),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getCapacity(),
                activity.getStatus().name(),
                registrationCount,
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
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

    private void notifyStudentsActivityPublished(ActivityEntity activity, ClubEntity club) {
        String title = trimToNull(activity.getTitle());
        String clubName = club == null ? "" : trimToNull(club.getName());
        String activityName = title == null ? "Untitled activity" : title;
        String content = clubName == null
                ? "A teacher published a new activity: " + activityName
                : "A teacher published a new activity for " + clubName + ": " + activityName;

        userRepository.findAllByRole(UserRole.STUDENT).stream()
                .filter(UserEntity::isEnabled)
                .map(UserEntity::getId)
                .distinct()
                .forEach(studentUserId -> notificationService.create(
                        studentUserId,
                        "ACTIVITY",
                        "activity published",
                        content,
                        "/student/activities"
                ));
    }
}
