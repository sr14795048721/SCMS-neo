package com.scms.core.club;

import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubDutyPermission;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.club.service.ClubDutyAccessService;
import com.scms.core.club.service.ClubDutySupport;
import com.scms.core.club.service.StudentCadreClubService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentCadreClubServiceTest {

    private ClubRepository clubRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ClubJoinRequestRepository clubJoinRequestRepository;
    private ActivityRepository activityRepository;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private ClubDutyAccessService clubDutyAccessService;
    private ClubDutySupport clubDutySupport;
    private StudentCadreClubService studentCadreClubService;

    @BeforeEach
    void setUp() {
        clubRepository = mock(ClubRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        clubJoinRequestRepository = mock(ClubJoinRequestRepository.class);
        activityRepository = mock(ActivityRepository.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        clubDutyAccessService = mock(ClubDutyAccessService.class);
        clubDutySupport = mock(ClubDutySupport.class);

        studentCadreClubService = new StudentCadreClubService(
                clubRepository,
                clubStudentMemberRepository,
                clubJoinRequestRepository,
                activityRepository,
                userRepository,
                studentInfoRepository,
                clubDutyAccessService,
                clubDutySupport
        );
    }

    @Test
    void getWorkspaceShouldRejectMemberWithoutDutyPermissions() {
        ClubStudentMemberEntity membership = new ClubStudentMemberEntity();
        membership.setClubId(1L);
        membership.setStudentUserId(10L);

        when(clubDutyAccessService.requireMembership(1L)).thenReturn(membership);
        when(clubDutyAccessService.getDuty(membership)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> studentCadreClubService.getWorkspace(1L));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void listMembersShouldRequireScoreManagementPermission() {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "student duty permission denied"))
                .when(clubDutyAccessService)
                .requirePermission(1L, ClubDutyPermission.SCORE_MANAGEMENT);

        BusinessException exception = assertThrows(BusinessException.class, () -> studentCadreClubService.listMembers(1L));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void getWorkspaceShouldAllowDutyWithPermissions() {
        ClubStudentMemberEntity membership = new ClubStudentMemberEntity();
        membership.setClubId(1L);
        membership.setStudentUserId(10L);
        membership.setDutyId(5L);

        ClubDutyEntity duty = new ClubDutyEntity();
        setDutyId(duty, 5L);
        duty.setName("积分负责人");
        duty.setPermissions(Set.of(ClubDutyPermission.SCORE_MANAGEMENT));

        when(clubDutyAccessService.requireMembership(1L)).thenReturn(membership);
        when(clubDutyAccessService.getDuty(membership)).thenReturn(duty);
        when(clubDutySupport.toPermissionValues(duty)).thenReturn(List.of("SCORE_MANAGEMENT"));
        when(clubStudentMemberRepository.findAllByClubId(1L)).thenReturn(List.of(membership));
        when(clubJoinRequestRepository.countByClubIdAndStatus(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(0L);
        when(clubJoinRequestRepository.findAllByClubIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of());
        when(activityRepository.findAllByClubIdInOrderByCreatedAtDescIdDesc(List.of(1L))).thenReturn(List.of());

        com.scms.core.club.domain.ClubEntity club = new com.scms.core.club.domain.ClubEntity();
        setClubId(club, 1L);
        club.setName("Art Club");
        club.setType("Art");
        club.setStatus(com.scms.core.club.domain.ClubStatus.ACTIVE);
        when(clubRepository.findById(1L)).thenReturn(java.util.Optional.of(club));

        assertEquals("积分负责人", studentCadreClubService.getWorkspace(1L).dutyName());
    }

    private void setClubId(com.scms.core.club.domain.ClubEntity entity, Long id) {
        try {
            var field = com.scms.core.club.domain.ClubEntity.class.getDeclaredField("id");
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
}
