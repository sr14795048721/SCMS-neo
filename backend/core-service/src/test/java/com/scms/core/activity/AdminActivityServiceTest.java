package com.scms.core.activity;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.activity.dto.AdminActivityMutationRequest;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.activity.service.AdminActivityService;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.event.DomainEventPublisher;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminActivityServiceTest {

    private ActivityRepository activityRepository;
    private ClubRepository clubRepository;
    private RegistrationRepository registrationRepository;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private DomainEventPublisher domainEventPublisher;
    private AdminActivityService adminActivityService;

    @BeforeEach
    void setUp() {
        activityRepository = mock(ActivityRepository.class);
        clubRepository = mock(ClubRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        domainEventPublisher = mock(DomainEventPublisher.class);

        adminActivityService = new AdminActivityService(
                activityRepository,
                clubRepository,
                registrationRepository,
                userRepository,
                studentInfoRepository,
                currentUserProvider,
                auditService,
                domainEventPublisher
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
    }

    @Test
    void createShouldDefaultToDraft() {
        ClubEntity club = buildClub(9L, "Art Club", ClubStatus.ACTIVE);
        when(clubRepository.findById(9L)).thenReturn(Optional.of(club));
        when(activityRepository.save(any(ActivityEntity.class))).thenAnswer((invocation) -> {
            ActivityEntity entity = invocation.getArgument(0);
            setActivityId(entity, 31L);
            return entity;
        });
        when(registrationRepository.findByActivityIdAndStatusOrderByCreatedAtDesc(31L, RegistrationStatus.REGISTERED))
                .thenReturn(List.of());

        var response = adminActivityService.create(new AdminActivityMutationRequest(
                9L,
                "New Activity",
                "desc",
                "Room 101",
                Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T12:00:00Z"),
                20
        ));

        assertEquals("DRAFT", response.status());
        verify(auditService).create(eq("ADMIN_ACTIVITY_CREATED"), eq(1L), eq("ACTIVITY"), eq("31"), any(String.class));
    }

    @Test
    void updatePublishedActivityShouldRejectCapacityBelowRegisteredCount() {
        ActivityEntity activity = buildActivity(11L, 9L, ActivityStatus.PUBLISHED);
        ClubEntity club = buildClub(9L, "Art Club", ClubStatus.ACTIVE);
        when(activityRepository.findById(11L)).thenReturn(Optional.of(activity));
        when(clubRepository.findById(9L)).thenReturn(Optional.of(club));
        when(registrationRepository.countByActivityIdAndStatus(11L, RegistrationStatus.REGISTERED)).thenReturn(5L);

        assertThrows(BusinessException.class, () -> adminActivityService.update(11L, new AdminActivityMutationRequest(
                9L,
                "Updated Activity",
                "desc",
                "Room 102",
                Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T12:00:00Z"),
                3
        )));
    }

    @Test
    void deleteShouldBlockWhenRegistrationsExist() {
        ActivityEntity activity = buildActivity(18L, 9L, ActivityStatus.DRAFT);
        when(activityRepository.findById(18L)).thenReturn(Optional.of(activity));
        when(registrationRepository.countByActivityId(18L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> adminActivityService.delete(18L));
    }

    private ClubEntity buildClub(Long id, String name, ClubStatus status) {
        ClubEntity club = new ClubEntity();
        setClubId(club, id);
        club.setName(name);
        club.setType("Art");
        club.setStatus(status);
        club.setDescription("desc");
        return club;
    }

    private ActivityEntity buildActivity(Long id, Long clubId, ActivityStatus status) {
        ActivityEntity activity = new ActivityEntity();
        setActivityId(activity, id);
        activity.setClubId(clubId);
        activity.setTitle("Activity");
        activity.setDescription("desc");
        activity.setLocation("Room 1");
        activity.setStartTime(Instant.parse("2030-01-01T10:00:00Z"));
        activity.setEndTime(Instant.parse("2030-01-01T12:00:00Z"));
        activity.setCapacity(20);
        activity.setStatus(status);
        activity.setCreatedBy(1L);
        return activity;
    }

    private void setClubId(ClubEntity club, Long id) {
        try {
            var field = ClubEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(club, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setActivityId(ActivityEntity activity, Long id) {
        try {
            var field = ActivityEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(activity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
