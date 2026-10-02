package com.scms.core.activity;

import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.activity.service.ManagerActivityService;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.event.DomainEventPublisher;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.repository.UserRepository;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

class ManagerActivityServiceTest {

    private ActivityRepository activityRepository;
    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private RegistrationRepository registrationRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private DomainEventPublisher domainEventPublisher;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private ManagerActivityService managerActivityService;

    @BeforeEach
    void setUp() {
        activityRepository = mock(ActivityRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        domainEventPublisher = mock(DomainEventPublisher.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);

        managerActivityService = new ManagerActivityService(
                activityRepository,
                clubRepository,
                clubManagerBindingRepository,
                registrationRepository,
                currentUserProvider,
                auditService,
                domainEventPublisher,
                userRepository,
                notificationService
        );
    }

    @Test
    void createShouldRejectClubOutsideManagerScope() {
        ClubEntity club = new ClubEntity();
        setClubId(club, 9L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(4L, "manager", UserRole.CLUB_MANAGER));
        when(clubRepository.findById(9L)).thenReturn(Optional.of(club));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(9L, 4L)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> managerActivityService.create(new AdminActivityMutationRequest(
                        9L,
                        "Activity",
                        "Description",
                        "Hall",
                        Instant.now().plusSeconds(7200),
                        Instant.now().plusSeconds(10800),
                        30
                ))
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void publishShouldNotifyEnabledStudents() {
        ClubEntity club = new ClubEntity();
        setClubId(club, 7L);
        club.setName("Technology Club");

        var activity = new com.scms.core.activity.domain.ActivityEntity();
        setActivityId(activity, 11L);
        activity.setClubId(7L);
        activity.setTitle("Robotics Workshop");
        activity.setStatus(com.scms.core.activity.domain.ActivityStatus.DRAFT);

        var enabledStudent = new com.scms.core.user.domain.UserEntity();
        setUserId(enabledStudent, 21L);
        enabledStudent.setRole(UserRole.STUDENT);
        enabledStudent.setEnabled(true);

        var disabledStudent = new com.scms.core.user.domain.UserEntity();
        setUserId(disabledStudent, 22L);
        disabledStudent.setRole(UserRole.STUDENT);
        disabledStudent.setEnabled(false);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(4L, "manager", UserRole.CLUB_MANAGER));
        when(activityRepository.findById(11L)).thenReturn(Optional.of(activity));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(7L, 4L)).thenReturn(true);
        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(activityRepository.save(activity)).thenReturn(activity);
        when(registrationRepository.countByActivityIdAndStatus(11L, com.scms.core.registration.domain.RegistrationStatus.REGISTERED)).thenReturn(0L);
        when(userRepository.findAllByRole(UserRole.STUDENT)).thenReturn(List.of(enabledStudent, disabledStudent));

        managerActivityService.publish(11L);

        verify(notificationService).create(
                21L,
                "ACTIVITY",
                "activity published",
                "A teacher published a new activity for Technology Club: Robotics Workshop",
                "/student/activities"
        );
    }

    private void setClubId(ClubEntity entity, Long id) {
        try {
            var field = ClubEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setActivityId(com.scms.core.activity.domain.ActivityEntity entity, Long id) {
        try {
            var field = com.scms.core.activity.domain.ActivityEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setUserId(com.scms.core.user.domain.UserEntity entity, Long id) {
        try {
            var field = com.scms.core.user.domain.UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
