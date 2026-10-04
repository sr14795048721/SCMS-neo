package com.scms.core.club;

import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubManagerBindingEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.AdminClubMutationRequest;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.club.service.AdminClubService;
import com.scms.core.club.service.ClubDutySupport;
import com.scms.core.homeclubrecommend.service.HomeClubRecommendationService;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminClubServiceTest {

    private ClubRepository clubRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private UserRepository userRepository;
    private ManagerInfoRepository managerInfoRepository;
    private StudentInfoRepository studentInfoRepository;
    private ActivityRepository activityRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private HomeClubRecommendationService homeClubRecommendationService;
    private ClubDutySupport clubDutySupport;
    private AdminClubService adminClubService;

    @BeforeEach
    void setUp() {
        clubRepository = mock(ClubRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        userRepository = mock(UserRepository.class);
        managerInfoRepository = mock(ManagerInfoRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        activityRepository = mock(ActivityRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        homeClubRecommendationService = mock(HomeClubRecommendationService.class);
        clubDutySupport = mock(ClubDutySupport.class);

        adminClubService = new AdminClubService(
                clubRepository,
                clubManagerBindingRepository,
                clubStudentMemberRepository,
                userRepository,
                managerInfoRepository,
                studentInfoRepository,
                activityRepository,
                currentUserProvider,
                auditService,
                homeClubRecommendationService,
                clubDutySupport
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
    }

    @Test
    void updateShouldOnlyAddNewManagerBindings() {
        ClubEntity club = new ClubEntity();
        setEntityId(club, 9L);
        club.setName("Art Club");
        club.setType("Art");
        club.setStatus(ClubStatus.ACTIVE);
        club.setDescription("desc");

        ClubManagerBindingEntity existingBinding = new ClubManagerBindingEntity();
        existingBinding.setClubId(9L);
        existingBinding.setManagerUserId(101L);

        UserEntity firstManager = buildManagerUser(101L, "teacher-a");
        UserEntity secondManager = buildManagerUser(102L, "teacher-b");

        when(clubRepository.findById(9L)).thenReturn(Optional.of(club));
        when(clubRepository.existsByNameIgnoreCaseAndIdNot("Art Club", 9L)).thenReturn(false);
        when(userRepository.findAllByIdInAndRole(List.of(101L, 102L), UserRole.CLUB_MANAGER)).thenReturn(List.of(firstManager, secondManager));
        when(clubManagerBindingRepository.findAllByClubId(9L)).thenReturn(List.of(existingBinding));
        when(clubRepository.save(any(ClubEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllByIdInAndRole(any(), eq(UserRole.CLUB_MANAGER))).thenReturn(List.of(firstManager, secondManager));
        when(managerInfoRepository.findAllByUserIdIn(any())).thenReturn(List.of());
        when(clubStudentMemberRepository.findAllByClubId(9L)).thenReturn(List.of());

        adminClubService.update(9L, new AdminClubMutationRequest(
                "Art Club",
                "Art",
                "ACTIVE",
                "desc",
                List.of(101L, 102L)
        ));

        ArgumentCaptor<List<ClubManagerBindingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(clubManagerBindingRepository, never()).deleteAllByClubId(9L);
        verify(clubManagerBindingRepository, never()).deleteAllByClubIdAndManagerUserIdIn(eq(9L), any());
        verify(clubManagerBindingRepository).saveAll(captor.capture());
        List<ClubManagerBindingEntity> savedBindings = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, savedBindings.size());
        org.junit.jupiter.api.Assertions.assertEquals(102L, savedBindings.get(0).getManagerUserId());
        verify(auditService).create(eq("ADMIN_CLUB_MANAGERS_UPDATED"), eq(1L), eq("CLUB"), eq("9"), any(String.class));
    }

    @Test
    void removeMemberShouldDeleteMembershipAndWriteAudit() {
        ClubEntity club = new ClubEntity();
        setEntityId(club, 9L);
        ClubStudentMemberEntity member = new ClubStudentMemberEntity();
        member.setClubId(9L);
        member.setStudentUserId(201L);

        when(clubRepository.findById(9L)).thenReturn(Optional.of(club));
        when(clubStudentMemberRepository.findByClubIdAndStudentUserId(9L, 201L)).thenReturn(Optional.of(member));

        adminClubService.removeMember(9L, 201L);

        verify(clubStudentMemberRepository).delete(member);
        verify(auditService, times(1)).create(eq("ADMIN_CLUB_MEMBER_REMOVED"), eq(1L), eq("CLUB_MEMBER"), eq("201"), any(String.class));
    }

    @Test
    void getDetailShouldTolerateDuplicateProfileRows() {
        ClubEntity club = new ClubEntity();
        setEntityId(club, 9L);
        club.setName("Art Club");
        club.setType("Art");
        club.setStatus(ClubStatus.ACTIVE);
        club.setDescription("desc");

        ClubManagerBindingEntity managerBinding = new ClubManagerBindingEntity();
        managerBinding.setClubId(9L);
        managerBinding.setManagerUserId(101L);

        ClubStudentMemberEntity studentMember = new ClubStudentMemberEntity();
        studentMember.setClubId(9L);
        studentMember.setStudentUserId(201L);

        UserEntity managerUser = buildManagerUser(101L, "teacher-a");
        UserEntity studentUser = buildStudentUser(201L, "student-a");

        ManagerInfoEntity olderManagerInfo = buildManagerInfo(101L, "旧老师", 1L);
        ManagerInfoEntity newerManagerInfo = buildManagerInfo(101L, "新老师", 2L);
        StudentInfoEntity olderStudentInfo = buildStudentInfo(201L, "旧同学", 1L);
        StudentInfoEntity newerStudentInfo = buildStudentInfo(201L, "新同学", 2L);

        when(clubRepository.findById(9L)).thenReturn(Optional.of(club));
        when(clubManagerBindingRepository.findAllByClubId(9L)).thenReturn(List.of(managerBinding));
        when(clubStudentMemberRepository.findAllByClubId(9L)).thenReturn(List.of(studentMember));
        when(userRepository.findAllByIdInAndRole(List.of(101L), UserRole.CLUB_MANAGER)).thenReturn(List.of(managerUser));
        when(userRepository.findAllByIdInAndRole(List.of(201L), UserRole.STUDENT)).thenReturn(List.of(studentUser));
        when(managerInfoRepository.findAllByUserIdIn(List.of(101L))).thenReturn(List.of(olderManagerInfo, newerManagerInfo));
        when(studentInfoRepository.findAllByUserIdIn(List.of(201L))).thenReturn(List.of(olderStudentInfo, newerStudentInfo));
        when(clubDutySupport.loadDutyMapByIds(List.of())).thenReturn(Map.of());

        var detail = adminClubService.getDetail(9L);

        org.junit.jupiter.api.Assertions.assertEquals(1, detail.managers().size());
        org.junit.jupiter.api.Assertions.assertEquals("新老师", detail.managers().get(0).displayName());
        org.junit.jupiter.api.Assertions.assertEquals(1, detail.students().size());
        org.junit.jupiter.api.Assertions.assertEquals("新同学", detail.students().get(0).displayName());
    }

    private UserEntity buildManagerUser(Long id, String username) {
        UserEntity user = new UserEntity();
        setUserId(user, id);
        user.setUsername(username);
        user.setEmail(username + "@scms.local");
        user.setRole(UserRole.CLUB_MANAGER);
        user.setEnabled(true);
        return user;
    }

    private UserEntity buildStudentUser(Long id, String username) {
        UserEntity user = new UserEntity();
        setUserId(user, id);
        user.setUsername(username);
        user.setEmail(username + "@scms.local");
        user.setRole(UserRole.STUDENT);
        user.setEnabled(true);
        return user;
    }

    private ManagerInfoEntity buildManagerInfo(Long userId, String displayName, Long id) {
        ManagerInfoEntity entity = new ManagerInfoEntity();
        setManagerInfoId(entity, id);
        entity.setUserId(userId);
        entity.setDisplayName(displayName);
        setUpdatedAt(entity, id);
        return entity;
    }

    private StudentInfoEntity buildStudentInfo(Long userId, String displayName, Long id) {
        StudentInfoEntity entity = new StudentInfoEntity();
        setStudentInfoId(entity, id);
        entity.setUserId(userId);
        entity.setDisplayName(displayName);
        setUpdatedAt(entity, id);
        return entity;
    }

    private void setEntityId(ClubEntity club, Long id) {
        try {
            var field = ClubEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(club, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setUserId(UserEntity user, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setManagerInfoId(ManagerInfoEntity entity, Long id) {
        try {
            var field = ManagerInfoEntity.class.getDeclaredField("id");
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
            field.set(entity, java.time.Instant.ofEpochSecond(offsetSeconds));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
