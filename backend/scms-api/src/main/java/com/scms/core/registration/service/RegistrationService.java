package com.scms.core.registration.service;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.event.DomainEvent;
import com.scms.core.event.DomainEventPublisher;
import com.scms.core.event.DomainEventTopics;
import com.scms.core.registration.domain.RegistrationEntity;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.registration.dto.ActivityRegistrationRosterResponse;
import com.scms.core.registration.dto.RegistrationResponse;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final CurrentUserProvider currentUserProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;

    public RegistrationService(RegistrationRepository registrationRepository,
                               ActivityRepository activityRepository,
                               ClubStudentMemberRepository clubStudentMemberRepository,
                               ClubManagerBindingRepository clubManagerBindingRepository,
                               CurrentUserProvider currentUserProvider,
                               DomainEventPublisher domainEventPublisher,
                               UserRepository userRepository,
                               StudentInfoRepository studentInfoRepository) {
        this.registrationRepository = registrationRepository;
        this.activityRepository = activityRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.currentUserProvider = currentUserProvider;
        this.domainEventPublisher = domainEventPublisher;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
    }

    @Transactional
    public RegistrationResponse register(Long activityId) {
        AuthenticatedUser user = requireStudent();
        ActivityEntity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "activity not found"));
        validateStudentActivityScope(activity, user.userId());

        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity is not open for registration");
        }

        RegistrationEntity registration = registrationRepository.findByActivityIdAndUserId(activityId, user.userId())
                .orElseGet(RegistrationEntity::new);

        if (registration.getId() != null && registration.getStatus() == RegistrationStatus.REGISTERED) {
            throw new BusinessException(ErrorCode.CONFLICT, "already registered");
        }

        long activeCount = registrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.REGISTERED);
        if (activeCount >= activity.getCapacity()) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity quota is full");
        }

        registration.setActivityId(activityId);
        registration.setUserId(user.userId());
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setCanceledAt(null);
        RegistrationEntity saved = registrationRepository.save(registration);

        publishRegistrationEvent(
                DomainEventTopics.REGISTRATION_CREATED,
                saved.getId(),
                activityId,
                user.userId(),
                user.userId()
        );
        return toResponse(saved);
    }

    @Transactional
    public RegistrationResponse cancel(Long activityId) {
        AuthenticatedUser user = requireStudent();
        ActivityEntity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "activity not found"));
        validateStudentActivityScope(activity, user.userId());
        RegistrationEntity registration = registrationRepository.findByActivityIdAndUserId(activityId, user.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "registration not found"));

        if (registration.getStatus() == RegistrationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.CONFLICT, "registration already canceled");
        }

        registration.setStatus(RegistrationStatus.CANCELED);
        registration.setCanceledAt(Instant.now());
        RegistrationEntity saved = registrationRepository.save(registration);

        publishRegistrationEvent(
                DomainEventTopics.REGISTRATION_CANCELED,
                saved.getId(),
                activityId,
                user.userId(),
                user.userId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationRosterResponse> listActive(Long activityId) {
        ActivityEntity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "activity not found"));
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        validateRosterScope(activity, user);

        List<RegistrationEntity> registrations = registrationRepository.findByActivityIdAndStatusOrderByCreatedAtDesc(
                activityId,
                RegistrationStatus.REGISTERED
        );
        List<Long> userIds = registrations.stream().map(RegistrationEntity::getUserId).distinct().toList();
        Map<Long, UserEntity> userMap = loadUserMap(userIds);
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(userIds);

        return registrations.stream()
                .map((registration) -> toRosterResponse(
                        registration,
                        userMap.get(registration.getUserId()),
                        studentInfoMap.get(registration.getUserId())
                ))
                .toList();
    }

    private void validateRosterScope(ActivityEntity activity, AuthenticatedUser user) {
        if (user.role().isSystemAdmin()) {
            return;
        }
        if (user.role() == UserRole.CLUB_MANAGER) {
            if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(activity.getClubId(), user.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "manager cannot access this activity");
            }
            return;
        }
        if (user.role() == UserRole.STUDENT) {
            validateStudentActivityScope(activity, user.userId());
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "registration roster access denied");
    }

    @Transactional(readOnly = true)
    public List<Long> listMyRegisteredActivityIds() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        return registrationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.userId(), RegistrationStatus.REGISTERED)
                .stream()
                .map(RegistrationEntity::getActivityId)
                .distinct()
                .toList();
    }

    private RegistrationResponse toResponse(RegistrationEntity entity) {
        return new RegistrationResponse(
                entity.getId(),
                entity.getActivityId(),
                entity.getUserId(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getCanceledAt()
        );
    }

    private ActivityRegistrationRosterResponse toRosterResponse(RegistrationEntity entity,
                                                               UserEntity user,
                                                               StudentInfoEntity studentInfo) {
        return new ActivityRegistrationRosterResponse(
                entity.getId(),
                entity.getUserId(),
                user == null ? "" : defaultString(user.getUsername()),
                studentInfo == null ? "" : defaultString(studentInfo.getDisplayName()),
                studentInfo == null ? "" : defaultString(studentInfo.getStudentNo()),
                studentInfo == null ? "" : defaultString(studentInfo.getGrade()),
                studentInfo == null ? "" : defaultString(studentInfo.getClassName()),
                entity.getCreatedAt()
        );
    }

    private Map<Long, UserEntity> loadUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserEntity> userMap = new LinkedHashMap<>();
        for (UserEntity user : userRepository.findAllByIdInAndRole(userIds, UserRole.STUDENT)) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    private Map<Long, StudentInfoEntity> loadStudentInfoMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, StudentInfoEntity> infoMap = new LinkedHashMap<>();
        for (StudentInfoEntity studentInfo : studentInfoRepository.findAllByUserIdIn(userIds)) {
            infoMap.put(studentInfo.getUserId(), studentInfo);
        }
        return infoMap;
    }

    private void publishRegistrationEvent(String topic, Long registrationId, Long activityId, Long userId, Long operatorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("registrationId", registrationId);
        payload.put("activityId", activityId);
        payload.put("userId", userId);
        payload.put("operatorId", operatorId);
        domainEventPublisher.publish(new DomainEvent(
                topic,
                String.valueOf(registrationId),
                Instant.now(),
                payload
        ));
    }

    private AuthenticatedUser requireStudent() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return user;
    }

    private void validateStudentActivityScope(ActivityEntity activity, Long userId) {
        if (!clubStudentMemberRepository.existsByStudentUserId(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student has not joined any club");
        }
        if (!clubStudentMemberRepository.existsByClubIdAndStudentUserId(activity.getClubId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "activity is outside the student's club");
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
