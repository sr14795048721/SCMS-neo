package com.scms.core.activity;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.activity.service.StudentActivityService;
import com.scms.core.club.domain.ClubDutyPermission;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.club.service.ClubDutyAccessService;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentActivityServiceTest {

    private ActivityRepository activityRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private CurrentUserProvider currentUserProvider;
    private ClubDutyAccessService clubDutyAccessService;
    private RegistrationRepository registrationRepository;
    private StudentActivityService studentActivityService;

    @BeforeEach
    void setUp() {
        activityRepository = mock(ActivityRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        clubDutyAccessService = mock(ClubDutyAccessService.class);
        registrationRepository = mock(RegistrationRepository.class);
        studentActivityService = new StudentActivityService(
                activityRepository,
                clubStudentMemberRepository,
                currentUserProvider,
                clubDutyAccessService,
                registrationRepository
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(12L, "student", UserRole.STUDENT));
    }

    @Test
    void listMyClubActivitiesShouldReturnEmptyWhenStudentHasNotJoinedClub() {
        when(clubStudentMemberRepository.findAllByStudentUserIdOrderByCreatedAtAscIdAsc(12L)).thenReturn(List.of());

        assertEquals(List.of(), studentActivityService.listMyClubActivities());
    }

    @Test
    void listMyClubActivitiesShouldReturnOnlyPrimaryClubActivities() {
        ClubStudentMemberEntity membership = new ClubStudentMemberEntity();
        membership.setClubId(7L);
        membership.setStudentUserId(12L);

        ActivityEntity activity = new ActivityEntity();
        setId(activity, 101L);
        activity.setClubId(7L);
        activity.setTitle("Tech Workshop");
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setCapacity(30);
        setCreatedAt(activity, Instant.parse("2026-04-01T10:00:00Z"));

        when(clubStudentMemberRepository.findAllByStudentUserIdOrderByCreatedAtAscIdAsc(12L))
                .thenReturn(List.of(membership));
        when(activityRepository.findAllStudentVisibleByClubId(7L)).thenReturn(List.of(activity));

        var result = studentActivityService.listMyClubActivities();

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).id());
        assertEquals(7L, result.get(0).clubId());
        assertEquals("Tech Workshop", result.get(0).title());
    }

    @Test
    void updateShouldRequireActivityManagementPermission() {
        ActivityEntity activity = new ActivityEntity();
        setId(activity, 9L);
        activity.setClubId(7L);
        activity.setStatus(ActivityStatus.DRAFT);

        when(activityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(activity));
        doThrow(new BusinessException(com.scms.core.common.error.ErrorCode.FORBIDDEN, "student duty permission denied"))
                .when(clubDutyAccessService)
                .requirePermission(7L, ClubDutyPermission.ACTIVITY_MANAGEMENT);

        assertThrows(
                BusinessException.class,
                () -> studentActivityService.update(9L, new AdminActivityMutationRequest(
                        7L,
                        "Updated Title",
                        "Updated description",
                        "Hall",
                        Instant.parse("2026-04-12T08:00:00Z"),
                        Instant.parse("2026-04-12T10:00:00Z"),
                        50
                ))
        );
    }

    @Test
    void updateShouldRejectChangingActivityClub() {
        ActivityEntity activity = new ActivityEntity();
        setId(activity, 9L);
        activity.setClubId(7L);
        activity.setStatus(ActivityStatus.DRAFT);

        when(activityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(activity));

        assertThrows(
                BusinessException.class,
                () -> studentActivityService.update(9L, new AdminActivityMutationRequest(
                        8L,
                        "Updated Title",
                        "Updated description",
                        "Hall",
                        Instant.parse("2026-04-12T08:00:00Z"),
                        Instant.parse("2026-04-12T10:00:00Z"),
                        50
                ))
        );
    }

    @Test
    void updateShouldPersistActivityWhenStudentHasPermission() {
        ActivityEntity activity = new ActivityEntity();
        setId(activity, 9L);
        activity.setClubId(7L);
        activity.setTitle("Before");
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setCapacity(40);

        when(activityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(activity));
        when(registrationRepository.countByActivityIdAndStatus(9L, RegistrationStatus.REGISTERED)).thenReturn(12L);
        when(activityRepository.save(activity)).thenReturn(activity);

        var result = studentActivityService.update(9L, new AdminActivityMutationRequest(
                7L,
                "After",
                "New description",
                "Gym",
                Instant.parse("2026-04-12T08:00:00Z"),
                Instant.parse("2026-04-12T10:00:00Z"),
                60
        ));

        verify(clubDutyAccessService).requirePermission(7L, ClubDutyPermission.ACTIVITY_MANAGEMENT);
        verify(registrationRepository).countByActivityIdAndStatus(9L, RegistrationStatus.REGISTERED);
        assertEquals("After", result.title());
        assertEquals("Gym", result.location());
        assertEquals(60, result.capacity());
    }

    private void setId(ActivityEntity entity, Long id) {
        try {
            var field = ActivityEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setCreatedAt(ActivityEntity entity, Instant createdAt) {
        try {
            var field = ActivityEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
