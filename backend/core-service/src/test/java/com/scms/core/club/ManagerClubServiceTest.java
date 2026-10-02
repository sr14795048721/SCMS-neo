package com.scms.core.club;

import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubJoinRequestEntity;
import com.scms.core.club.domain.ClubJoinRequestStatus;
import com.scms.core.club.domain.ClubManagerBindingEntity;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.ManagerClubJoinRequestResponse;
import com.scms.core.club.dto.ManagerClubMemberResponse;
import com.scms.core.club.dto.StudentClubJoinRequestCreateRequest;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.club.service.ClubDutySupport;
import com.scms.core.club.service.ManagerClubService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerClubServiceTest {

    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ClubJoinRequestRepository clubJoinRequestRepository;
    private ActivityRepository activityRepository;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private ManagerInfoRepository managerInfoRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private NotificationService notificationService;
    private ClubDutySupport clubDutySupport;
    private ManagerClubService managerClubService;

    @BeforeEach
    void setUp() {
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        clubJoinRequestRepository = mock(ClubJoinRequestRepository.class);
        activityRepository = mock(ActivityRepository.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        managerInfoRepository = mock(ManagerInfoRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        notificationService = mock(NotificationService.class);
        clubDutySupport = mock(ClubDutySupport.class);

        managerClubService = new ManagerClubService(
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                clubJoinRequestRepository,
                activityRepository,
                userRepository,
                studentInfoRepository,
                managerInfoRepository,
                currentUserProvider,
                auditService,
                notificationService,
                clubDutySupport
        );

        when(userRepository.findAllByIdInAndRole(any(), any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(studentInfoRepository.findAllByUserIdIn(any())).thenReturn(List.of());
        when(managerInfoRepository.findAllByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.findByIdForUpdate(any())).thenAnswer(invocation -> Optional.of(studentUser(invocation.getArgument(0, Long.class))));
    }

    @Test
    void createStudentJoinRequestShouldRejectDuplicatePendingRequest() {
        ClubEntity club = activeClub(7L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(12L, "student", UserRole.STUDENT));
        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubStudentMemberRepository.existsByStudentUserId(12L)).thenReturn(false);
        when(clubJoinRequestRepository.existsByStudentUserIdAndStatus(12L, ClubJoinRequestStatus.PENDING)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> managerClubService.createStudentJoinRequest(new StudentClubJoinRequestCreateRequest(7L, "apply"))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void approveJoinRequestShouldCreateMemberAndMarkApproved() {
        ClubJoinRequestEntity request = new ClubJoinRequestEntity();
        setJoinRequestId(request, 8L);
        request.setClubId(3L);
        request.setStudentUserId(22L);
        request.setStatus(ClubJoinRequestStatus.PENDING);
        request.setReason("join");
        setJoinRequestTimestamps(request);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 5L)).thenReturn(true);
        when(clubJoinRequestRepository.findByIdAndClubId(8L, 3L)).thenReturn(Optional.of(request));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(3L, 22L)).thenReturn(false);
        when(clubStudentMemberRepository.saveAndFlush(any(ClubStudentMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clubJoinRequestRepository.save(any(ClubJoinRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManagerClubJoinRequestResponse response = managerClubService.approveJoinRequest(3L, 8L);

        assertEquals("APPROVED", response.status());
        verify(clubStudentMemberRepository).saveAndFlush(any(ClubStudentMemberEntity.class));
        verify(clubJoinRequestRepository).save(request);
    }

    @Test
    void createStudentJoinRequestShouldNotifyManagers() {
        ClubEntity club = activeClub(7L);
        club.setName("Art Club");

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(12L, "student", UserRole.STUDENT));
        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubStudentMemberRepository.existsByStudentUserId(12L)).thenReturn(false);
        when(clubJoinRequestRepository.existsByStudentUserIdAndStatus(12L, ClubJoinRequestStatus.PENDING)).thenReturn(false);
        when(clubManagerBindingRepository.findAllByClubId(7L)).thenReturn(List.of(binding(7L, 5L)));
        when(clubJoinRequestRepository.saveAndFlush(any(ClubJoinRequestEntity.class))).thenAnswer(invocation -> {
            ClubJoinRequestEntity entity = invocation.getArgument(0);
            setJoinRequestId(entity, 15L);
            setJoinRequestTimestamps(entity);
            return entity;
        });

        ManagerClubJoinRequestResponse response =
                managerClubService.createStudentJoinRequest(new StudentClubJoinRequestCreateRequest(7L, "apply"));

        assertEquals("PENDING", response.status());
        verify(notificationService).create(
                5L,
                "CLUB_JOIN_REQUEST",
                "new join request",
                "A student submitted a new join request for Art Club.",
                "/club-admin/clubs/7"
        );
    }

    @Test
    void createStudentJoinRequestShouldRejectStudentAlreadyInAnotherClub() {
        ClubEntity club = activeClub(7L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(12L, "student", UserRole.STUDENT));
        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubStudentMemberRepository.existsByStudentUserId(12L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> managerClubService.createStudentJoinRequest(new StudentClubJoinRequestCreateRequest(7L, "apply"))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void approveJoinRequestShouldRejectStudentAlreadyInAnotherClub() {
        ClubJoinRequestEntity request = new ClubJoinRequestEntity();
        setJoinRequestId(request, 8L);
        request.setClubId(3L);
        request.setStudentUserId(22L);
        request.setStatus(ClubJoinRequestStatus.PENDING);
        setJoinRequestTimestamps(request);

        ClubStudentMemberEntity existingMembership = new ClubStudentMemberEntity();
        existingMembership.setClubId(9L);
        existingMembership.setStudentUserId(22L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 5L)).thenReturn(true);
        when(clubJoinRequestRepository.findByIdAndClubId(8L, 3L)).thenReturn(Optional.of(request));
        when(clubStudentMemberRepository.findAllByStudentUserId(22L)).thenReturn(List.of(existingMembership));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> managerClubService.approveJoinRequest(3L, 8L)
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void createStudentJoinRequestShouldTranslateUniqueConstraintViolation() {
        ClubEntity club = activeClub(7L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(12L, "student", UserRole.STUDENT));
        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubStudentMemberRepository.existsByStudentUserId(12L)).thenReturn(false);
        when(clubJoinRequestRepository.existsByStudentUserIdAndStatus(12L, ClubJoinRequestStatus.PENDING)).thenReturn(false);
        when(clubJoinRequestRepository.saveAndFlush(any(ClubJoinRequestEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate pending request"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> managerClubService.createStudentJoinRequest(new StudentClubJoinRequestCreateRequest(7L, "apply"))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("join request is already pending", exception.getMessage());
    }

    @Test
    void approveJoinRequestShouldTranslateMembershipUniqueConstraintViolation() {
        ClubJoinRequestEntity request = new ClubJoinRequestEntity();
        setJoinRequestId(request, 8L);
        request.setClubId(3L);
        request.setStudentUserId(22L);
        request.setStatus(ClubJoinRequestStatus.PENDING);
        setJoinRequestTimestamps(request);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 5L)).thenReturn(true);
        when(clubJoinRequestRepository.findByIdAndClubId(8L, 3L)).thenReturn(Optional.of(request));
        when(clubStudentMemberRepository.existsByClubIdAndStudentUserId(3L, 22L)).thenReturn(false);
        when(clubStudentMemberRepository.saveAndFlush(any(ClubStudentMemberEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate membership"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> managerClubService.approveJoinRequest(3L, 8L)
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("student is already a member of another club", exception.getMessage());
    }

    @Test
    void removeMemberShouldDeleteMembershipAndWriteAudit() {
        ClubStudentMemberEntity member = new ClubStudentMemberEntity();
        member.setClubId(3L);
        member.setStudentUserId(22L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 5L)).thenReturn(true);
        when(clubStudentMemberRepository.findByClubIdAndStudentUserId(3L, 22L)).thenReturn(Optional.of(member));

        managerClubService.removeMember(3L, 22L);

        verify(clubStudentMemberRepository).delete(member);
        verify(auditService, times(1)).create(eq("MANAGER_CLUB_MEMBER_REMOVED"), eq(5L), eq("CLUB_MEMBER"), eq("22"), any(String.class));
    }

    @Test
    void listMembersShouldTolerateNullDutyNames() {
        ClubStudentMemberEntity member = new ClubStudentMemberEntity();
        member.setClubId(3L);
        member.setStudentUserId(22L);
        member.setDutyId(8L);

        UserEntity user = studentUser(22L);

        StudentInfoEntity info = new StudentInfoEntity();
        info.setUserId(22L);
        info.setDisplayName("member-name");

        ClubDutyEntity duty = new ClubDutyEntity();
        setDutyId(duty, 8L);
        duty.setClubId(3L);
        duty.setName(null);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 5L)).thenReturn(true);
        when(clubStudentMemberRepository.findAllByClubId(3L)).thenReturn(List.of(member));
        when(userRepository.findAllByIdInAndRole(List.of(22L), UserRole.STUDENT)).thenReturn(List.of(user));
        when(studentInfoRepository.findAllByUserIdIn(List.of(22L))).thenReturn(List.of(info));
        when(clubDutySupport.loadDutyMapByIds(List.of(8L))).thenReturn(Map.of(8L, duty));

        List<ManagerClubMemberResponse> result = managerClubService.listMembers(3L);

        assertEquals(1, result.size());
        assertEquals("member-name", result.getFirst().displayName());
    }

    @Test
    void listMembersShouldTolerateDuplicateProfileRows() {
        ClubStudentMemberEntity member = new ClubStudentMemberEntity();
        member.setClubId(3L);
        member.setStudentUserId(22L);

        UserEntity studentUser = studentUser(22L);

        StudentInfoEntity olderStudentInfo = new StudentInfoEntity();
        setStudentInfoId(olderStudentInfo, 1L);
        olderStudentInfo.setUserId(22L);
        olderStudentInfo.setDisplayName("older-name");
        setUpdatedAt(olderStudentInfo, 1L);

        StudentInfoEntity newerStudentInfo = new StudentInfoEntity();
        setStudentInfoId(newerStudentInfo, 2L);
        newerStudentInfo.setUserId(22L);
        newerStudentInfo.setDisplayName("newer-name");
        setUpdatedAt(newerStudentInfo, 2L);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(5L, "manager", UserRole.CLUB_MANAGER));
        when(clubManagerBindingRepository.existsByClubIdAndManagerUserId(3L, 5L)).thenReturn(true);
        when(clubStudentMemberRepository.findAllByClubId(3L)).thenReturn(List.of(member));
        when(userRepository.findAllByIdInAndRole(List.of(22L), UserRole.STUDENT)).thenReturn(List.of(studentUser));
        when(studentInfoRepository.findAllByUserIdIn(List.of(22L))).thenReturn(List.of(olderStudentInfo, newerStudentInfo));
        when(clubDutySupport.loadDutyMapByIds(List.of())).thenReturn(Map.of());

        List<ManagerClubMemberResponse> result = managerClubService.listMembers(3L);

        assertEquals(1, result.size());
        assertEquals("newer-name", result.getFirst().displayName());
    }

    private ClubEntity activeClub(Long clubId) {
        ClubEntity club = new ClubEntity();
        setClubId(club, clubId);
        club.setStatus(com.scms.core.club.domain.ClubStatus.ACTIVE);
        return club;
    }

    private ClubManagerBindingEntity binding(Long clubId, Long managerUserId) {
        ClubManagerBindingEntity entity = new ClubManagerBindingEntity();
        entity.setClubId(clubId);
        entity.setManagerUserId(managerUserId);
        return entity;
    }

    private UserEntity studentUser(Long id) {
        UserEntity user = new UserEntity();
        setUserId(user, id);
        user.setUsername("student-" + id);
        user.setRole(UserRole.STUDENT);
        return user;
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

    private void setDutyId(ClubDutyEntity entity, Long id) {
        try {
            var field = ClubDutyEntity.class.getDeclaredField("id");
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

    private void setStudentInfoId(StudentInfoEntity entity, Long id) {
        try {
            var field = StudentInfoEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setUpdatedAt(Object entity, Long offsetSeconds) {
        try {
            var field = entity.getClass().getDeclaredField("updatedAt");
            field.setAccessible(true);
            field.set(entity, Instant.ofEpochSecond(offsetSeconds));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setJoinRequestId(ClubJoinRequestEntity entity, Long id) {
        try {
            var field = ClubJoinRequestEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setJoinRequestTimestamps(ClubJoinRequestEntity entity) {
        try {
            var createdAt = ClubJoinRequestEntity.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(entity, Instant.now());
            var updatedAt = ClubJoinRequestEntity.class.getDeclaredField("updatedAt");
            updatedAt.setAccessible(true);
            updatedAt.set(entity, Instant.now());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
