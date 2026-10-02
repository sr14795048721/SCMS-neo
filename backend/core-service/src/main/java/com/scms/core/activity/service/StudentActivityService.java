package com.scms.core.activity.service;

import com.scms.core.activity.dto.ActivityResponse;
import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.club.domain.ClubDutyPermission;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.club.service.ClubDutyAccessService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentActivityService {

    private final ActivityRepository activityRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ClubDutyAccessService clubDutyAccessService;
    private final RegistrationRepository registrationRepository;

    public StudentActivityService(ActivityRepository activityRepository,
                                  ClubStudentMemberRepository clubStudentMemberRepository,
                                  CurrentUserProvider currentUserProvider,
                                  ClubDutyAccessService clubDutyAccessService,
                                  RegistrationRepository registrationRepository) {
        this.activityRepository = activityRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.clubDutyAccessService = clubDutyAccessService;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listMyClubActivities() {
        AuthenticatedUser student = requireStudent();
        List<ClubStudentMemberEntity> memberships =
                clubStudentMemberRepository.findAllByStudentUserIdOrderByCreatedAtAscIdAsc(student.userId());
        if (memberships.isEmpty()) {
            return List.of();
        }

        Long clubId = memberships.get(0).getClubId();
        return activityRepository.findAllStudentVisibleByClubId(clubId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ActivityResponse update(Long activityId, AdminActivityMutationRequest request) {
        com.scms.core.activity.domain.ActivityEntity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "activity not found"));
        requireManageableActivity(activity, request.clubId());

        long registeredCount = registrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.REGISTERED);
        validateMutationRequest(request, registeredCount, activity.getStatus() == com.scms.core.activity.domain.ActivityStatus.PUBLISHED);

        activity.setTitle(normalizeTitle(request.title()));
        activity.setDescription(trimToNull(request.description()));
        activity.setLocation(trimToNull(request.location()));
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setCapacity(request.capacity());

        return toResponse(activityRepository.save(activity));
    }

    private AuthenticatedUser requireStudent() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return user;
    }

    private void requireManageableActivity(com.scms.core.activity.domain.ActivityEntity activity, Long requestClubId) {
        if (requestClubId == null || !requestClubId.equals(activity.getClubId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "student cannot change activity club");
        }
        clubDutyAccessService.requirePermission(activity.getClubId(), ClubDutyPermission.ACTIVITY_MANAGEMENT);
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

    private ActivityResponse toResponse(com.scms.core.activity.domain.ActivityEntity entity) {
        return new ActivityResponse(
                entity.getId(),
                entity.getClubId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCapacity(),
                entity.getStatus().name(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }
}
