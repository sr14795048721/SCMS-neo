package com.scms.core.admin;

import com.scms.core.admin.dto.AdminDirtyDataCleanupResponse;
import com.scms.core.admin.service.AdminSystemMaintenanceService;
import com.scms.core.attendance.repository.AttendanceRecordRepository;
import com.scms.core.attendance.repository.AttendanceSessionRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.club.repository.ClubDutyRepository;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.notification.repository.NotificationRepository;
import com.scms.core.registration.repository.RegistrationRepository;
import com.scms.core.reward.repository.RewardItemTargetClubRepository;
import com.scms.core.reward.repository.RewardOrderRepository;
import com.scms.core.score.repository.ScoreRecordRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSystemMaintenanceServiceTest {

    private AttendanceRecordRepository attendanceRecordRepository;
    private AttendanceSessionRepository attendanceSessionRepository;
    private ScoreRecordRepository scoreRecordRepository;
    private RewardOrderRepository rewardOrderRepository;
    private ClubJoinRequestRepository clubJoinRequestRepository;
    private RegistrationRepository registrationRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private NotificationRepository notificationRepository;
    private StudentInfoRepository studentInfoRepository;
    private ManagerInfoRepository managerInfoRepository;
    private ClubManagerBindingRepository clubManagerBindingRepository;
    private ClubDutyRepository clubDutyRepository;
    private RewardItemTargetClubRepository rewardItemTargetClubRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private AdminSystemMaintenanceService service;

    @BeforeEach
    void setUp() {
        attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        attendanceSessionRepository = mock(AttendanceSessionRepository.class);
        scoreRecordRepository = mock(ScoreRecordRepository.class);
        rewardOrderRepository = mock(RewardOrderRepository.class);
        clubJoinRequestRepository = mock(ClubJoinRequestRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        managerInfoRepository = mock(ManagerInfoRepository.class);
        clubManagerBindingRepository = mock(ClubManagerBindingRepository.class);
        clubDutyRepository = mock(ClubDutyRepository.class);
        rewardItemTargetClubRepository = mock(RewardItemTargetClubRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        service = new AdminSystemMaintenanceService(
                attendanceRecordRepository,
                attendanceSessionRepository,
                scoreRecordRepository,
                rewardOrderRepository,
                clubJoinRequestRepository,
                registrationRepository,
                clubStudentMemberRepository,
                notificationRepository,
                studentInfoRepository,
                managerInfoRepository,
                clubManagerBindingRepository,
                clubDutyRepository,
                rewardItemTargetClubRepository,
                currentUserProvider,
                auditService
        );
    }

    @Test
    void cleanDirtyDataShouldRejectNonAdminUser() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(2L, "student", UserRole.STUDENT));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.cleanDirtyData());

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void cleanDirtyDataShouldAggregateCleanupCounts() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
        when(attendanceRecordRepository.deleteOrphanedRecords()).thenReturn(2);
        when(attendanceSessionRepository.deleteOrphanedSessions()).thenReturn(1);
        when(clubManagerBindingRepository.deleteOrphanedBindings()).thenReturn(1);
        when(clubDutyRepository.deleteOrphanedDuties()).thenReturn(2);
        when(rewardItemTargetClubRepository.deleteOrphanedTargets()).thenReturn(3);
        when(clubStudentMemberRepository.deleteOrphanedMemberships()).thenReturn(4);
        when(clubJoinRequestRepository.deleteOrphanedJoinRequests()).thenReturn(5);
        when(registrationRepository.deleteOrphanedRegistrations()).thenReturn(6);
        when(scoreRecordRepository.deleteOrphanedRecords()).thenReturn(7);
        when(rewardOrderRepository.deleteOrphanedOrders()).thenReturn(8);
        when(notificationRepository.deleteOrphanedNotifications()).thenReturn(9);
        when(studentInfoRepository.deleteOrphanedStudentProfiles()).thenReturn(10);
        when(managerInfoRepository.deleteOrphanedManagerProfiles()).thenReturn(11);

        AdminDirtyDataCleanupResponse response = service.cleanDirtyData();

        assertEquals(2, response.attendanceRecordsRemoved());
        assertEquals(1, response.attendanceSessionsRemoved());
        assertEquals(7, response.scoreRecordsRemoved());
        assertEquals(8, response.rewardOrdersRemoved());
        assertEquals(5, response.clubJoinRequestsRemoved());
        assertEquals(6, response.registrationsRemoved());
        assertEquals(4, response.clubMembersRemoved());
        assertEquals(9, response.notificationsRemoved());
        assertEquals(10, response.studentProfilesRemoved());
        assertEquals(11, response.managerProfilesRemoved());
        assertEquals(1, response.clubManagerBindingsRemoved());
        assertEquals(2, response.clubDutiesRemoved());
        assertEquals(3, response.rewardTargetClubsRemoved());
        assertEquals(69, response.totalRemoved());
        verify(auditService).create(
                "ADMIN_DIRTY_DATA_CLEANED",
                1L,
                "SYSTEM",
                "DIRTY_DATA",
                "Administrator cleaned dirty data and removed 69 records"
        );
    }
}
