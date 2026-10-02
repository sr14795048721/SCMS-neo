package com.scms.core.registration;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.event.DomainEventPublisher;
import com.scms.core.registration.domain.RegistrationEntity;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.registration.service.RegistrationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private RegistrationRepository registrationRepository;
    private ActivityRepository activityRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private CurrentUserProvider currentUserProvider;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationRepository = mock(RegistrationRepository.class);
        activityRepository = mock(ActivityRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        registrationService = new RegistrationService(
                registrationRepository,
                activityRepository,
                clubStudentMemberRepository,
                clubManagerBindingRepository,
                currentUserProvider,
                mock(DomainEventPublisher.class),
                userRepository,
                studentInfoRepository
        );
    }

    @Test
    void registerShouldThrowWhenQuotaIsFull() {
        ActivityEntity activity = new ActivityEntity();
        activity.setClubId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setCapacity(1);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "stu", UserRole.STUDENT));
        when(activityRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activity));
        when(clubStudentMemberRepository.existsByStudentUserId(2L)).thenReturn(true);
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(7L, 2L)).thenReturn(true);
        when(registrationRepository.findByActivityIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(registrationRepository.countByActivityIdAndStatus(1L, RegistrationStatus.REGISTERED)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> registrationService.register(1L));
    }

    @Test
    void cancelShouldThrowWhenNotFound() {
        ActivityEntity activity = new ActivityEntity();
        activity.setClubId(7L);
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "stu", UserRole.STUDENT));
        when(registrationRepository.findByActivityIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(clubStudentMemberRepository.existsByStudentUserId(2L)).thenReturn(true);
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(7L, 2L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> registrationService.cancel(1L));
    }

    @Test
    void listMyRegisteredActivityIdsShouldReturnDistinctIds() {
        RegistrationEntity first = new RegistrationEntity();
        first.setActivityId(11L);
        first.setUserId(2L);
        first.setStatus(RegistrationStatus.REGISTERED);

        RegistrationEntity second = new RegistrationEntity();
        second.setActivityId(12L);
        second.setUserId(2L);
        second.setStatus(RegistrationStatus.REGISTERED);

        RegistrationEntity duplicate = new RegistrationEntity();
        duplicate.setActivityId(11L);
        duplicate.setUserId(2L);
        duplicate.setStatus(RegistrationStatus.REGISTERED);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "stu", UserRole.STUDENT));
        when(registrationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(2L, RegistrationStatus.REGISTERED))
                .thenReturn(List.of(first, second, duplicate));

        assertEquals(List.of(11L, 12L), registrationService.listMyRegisteredActivityIds());
    }

    @Test
    void registerShouldThrowWhenStudentHasNotJoinedAnyClub() {
        ActivityEntity activity = new ActivityEntity();
        activity.setClubId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setCapacity(10);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "stu", UserRole.STUDENT));
        when(activityRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activity));
        when(clubStudentMemberRepository.existsByStudentUserId(2L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> registrationService.register(1L));
    }

    @Test
    void registerShouldThrowWhenActivityIsOutsideStudentClub() {
        ActivityEntity activity = new ActivityEntity();
        activity.setClubId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setCapacity(10);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "stu", UserRole.STUDENT));
        when(activityRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activity));
        when(clubStudentMemberRepository.existsByStudentUserId(2L)).thenReturn(true);
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(7L, 2L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> registrationService.register(1L));
    }

    @Test
    void listActiveShouldAllowStudentInSameClubAndReturnRosterDetails() {
        ActivityEntity activity = new ActivityEntity();
        activity.setClubId(7L);

        RegistrationEntity registration = new RegistrationEntity();
        registration.setActivityId(1L);
        registration.setUserId(2L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        setRegistrationId(registration, 21L);

        UserEntity user = new UserEntity();
        setUserId(user, 2L);
        user.setUsername("stu-2");
        user.setRole(UserRole.STUDENT);

        StudentInfoEntity studentInfo = new StudentInfoEntity();
        studentInfo.setUserId(2L);
        studentInfo.setDisplayName("Alice");
        studentInfo.setStudentNo("20260001");
        studentInfo.setGrade("HIGH_1");
        studentInfo.setClassName("1");

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "stu-2", UserRole.STUDENT));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(clubStudentMemberRepository.existsByStudentUserId(2L)).thenReturn(true);
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(7L, 2L)).thenReturn(true);
        when(registrationRepository.findByActivityIdAndStatusOrderByCreatedAtDesc(1L, RegistrationStatus.REGISTERED))
                .thenReturn(List.of(registration));
        when(userRepository.findAllByIdInAndRole(List.of(2L), UserRole.STUDENT)).thenReturn(List.of(user));
        when(studentInfoRepository.findAllByUserIdIn(List.of(2L))).thenReturn(List.of(studentInfo));

        var result = registrationService.listActive(1L);

        assertEquals(1, result.size());
        assertEquals(21L, result.get(0).registrationId());
        assertEquals("Alice", result.get(0).displayName());
        assertEquals("20260001", result.get(0).studentNo());
    }

    @Test
    void listActiveShouldRejectManagerOutsideClub() {
        ActivityEntity activity = new ActivityEntity();
        activity.setClubId(7L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(8L, "manager", UserRole.CLUB_MANAGER));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(7L, 8L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> registrationService.listActive(1L));
    }

    private void setRegistrationId(RegistrationEntity entity, Long id) {
        try {
            var field = RegistrationEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setUserId(UserEntity entity, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
