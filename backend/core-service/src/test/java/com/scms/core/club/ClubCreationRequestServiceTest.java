package com.scms.core.club;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubCreationRequestEntity;
import com.scms.core.club.domain.ClubCreationRequestStatus;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubManagerBindingEntity;
import com.scms.core.club.dto.ClubCreationRequestResponse;
import com.scms.core.club.dto.CreateClubCreationRequestRequest;
import com.scms.core.club.repository.ClubCreationRequestRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.service.ClubCreationRequestService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClubCreationRequestServiceTest {

    private ClubCreationRequestRepository clubCreationRequestRepository;
    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private UserRepository userRepository;
    private ManagerInfoRepository managerInfoRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private NotificationService notificationService;
    private ClubCreationRequestService clubCreationRequestService;

    @BeforeEach
    void setUp() {
        clubCreationRequestRepository = mock(ClubCreationRequestRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        userRepository = mock(UserRepository.class);
        managerInfoRepository = mock(ManagerInfoRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        notificationService = mock(NotificationService.class);
        clubCreationRequestService = new ClubCreationRequestService(
                clubCreationRequestRepository,
                clubRepository,
                clubManagerBindingRepository,
                userRepository,
                managerInfoRepository,
                currentUserProvider,
                auditService,
                notificationService
        );

        when(managerInfoRepository.findAllByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.findAllByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN))).thenReturn(List.of(adminUser(1L)));
    }

    @Test
    void createManagerRequestShouldRejectDuplicatePendingRequest() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(7L, "manager", UserRole.CLUB_MANAGER));
        when(clubCreationRequestRepository.existsByApplicantManagerUserIdAndStatus(7L, ClubCreationRequestStatus.PENDING)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> clubCreationRequestService.createManagerRequest(new CreateClubCreationRequestRequest("A", "Type", "Desc", "Reason"))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void createManagerRequestShouldReturnPendingResponseWhenReviewerIsNull() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(7L, "manager", UserRole.CLUB_MANAGER));
        when(clubCreationRequestRepository.existsByApplicantManagerUserIdAndStatus(7L, ClubCreationRequestStatus.PENDING)).thenReturn(false);
        when(clubRepository.existsByNameIgnoreCase("New Club")).thenReturn(false);
        when(clubCreationRequestRepository.save(any(ClubCreationRequestEntity.class))).thenAnswer(invocation -> {
            ClubCreationRequestEntity entity = invocation.getArgument(0);
            setRequestId(entity, 18L);
            setRequestTimestamps(entity);
            return entity;
        });
        when(userRepository.findAllByIdInAndRole(List.of(7L), UserRole.CLUB_MANAGER)).thenReturn(List.of(managerUser(7L)));

        ClubCreationRequestResponse response = clubCreationRequestService.createManagerRequest(
                new CreateClubCreationRequestRequest("New Club", "Culture", "desc", "reason")
        );

        assertEquals("PENDING", response.status());
        assertEquals("", response.reviewedByName());
        verify(notificationService).create(
                1L,
                "CLUB_CREATION_REQUEST",
                "new club creation request",
                "A club manager submitted a new club creation request: New Club",
                "/sys-admin/club-manage?tab=requests"
        );
    }

    @Test
    void approveRequestShouldCreateClubAndBindApplicant() {
        ClubCreationRequestEntity entity = new ClubCreationRequestEntity();
        setRequestId(entity, 9L);
        entity.setApplicantManagerUserId(7L);
        entity.setName("New Club");
        entity.setType("Culture");
        entity.setDescription("desc");
        entity.setApplyReason("reason");
        entity.setStatus(ClubCreationRequestStatus.PENDING);
        setRequestTimestamps(entity);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
        when(clubCreationRequestRepository.findById(9L)).thenReturn(Optional.of(entity));
        when(clubRepository.save(any(ClubEntity.class))).thenAnswer(invocation -> {
            ClubEntity club = invocation.getArgument(0);
            setClubId(club, 33L);
            return club;
        });
        when(clubCreationRequestRepository.save(any(ClubCreationRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllByIdInAndRole(List.of(7L), UserRole.CLUB_MANAGER)).thenReturn(List.of(managerUser(7L)));

        ClubCreationRequestResponse response = clubCreationRequestService.approveRequest(9L);

        assertEquals("APPROVED", response.status());
        verify(clubManagerBindingRepository).save(any(ClubManagerBindingEntity.class));
        verify(notificationService).create(
                7L,
                "CLUB_CREATION_REQUEST",
                "club creation approved",
                "Your club creation request has been approved.",
                "/club-admin/clubs/33"
        );
    }

    private UserEntity managerUser(Long id) {
        UserEntity user = new UserEntity();
        setUserId(user, id);
        user.setUsername("manager");
        user.setEmail("manager@scms.local");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.CLUB_MANAGER);
        user.setEnabled(true);
        return user;
    }

    private UserEntity adminUser(Long id) {
        UserEntity user = new UserEntity();
        setUserId(user, id);
        user.setUsername("admin");
        user.setEmail("admin@scms.local");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        return user;
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

    private void setClubId(ClubEntity entity, Long id) {
        try {
            var field = ClubEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setRequestId(ClubCreationRequestEntity entity, Long id) {
        try {
            var field = ClubCreationRequestEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setRequestTimestamps(ClubCreationRequestEntity entity) {
        try {
            var createdAt = ClubCreationRequestEntity.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(entity, Instant.now());
            var updatedAt = ClubCreationRequestEntity.class.getDeclaredField("updatedAt");
            updatedAt.setAccessible(true);
            updatedAt.set(entity, Instant.now());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
