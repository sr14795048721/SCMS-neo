package com.scms.core.admin.service;

import com.scms.core.admin.dto.AdminDirtyDataCleanupResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSystemMaintenanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final ScoreRecordRepository scoreRecordRepository;
    private final RewardOrderRepository rewardOrderRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final RegistrationRepository registrationRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final NotificationRepository notificationRepository;
    private final com.scms.core.student.repository.StudentInfoRepository studentInfoRepository;
    private final ManagerInfoRepository managerInfoRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final ClubDutyRepository clubDutyRepository;
    private final RewardItemTargetClubRepository rewardItemTargetClubRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public AdminSystemMaintenanceService(AttendanceRecordRepository attendanceRecordRepository,
                                         AttendanceSessionRepository attendanceSessionRepository,
                                         ScoreRecordRepository scoreRecordRepository,
                                         RewardOrderRepository rewardOrderRepository,
                                         ClubJoinRequestRepository clubJoinRequestRepository,
                                         RegistrationRepository registrationRepository,
                                         ClubStudentMemberRepository clubStudentMemberRepository,
                                         NotificationRepository notificationRepository,
                                         com.scms.core.student.repository.StudentInfoRepository studentInfoRepository,
                                         ManagerInfoRepository managerInfoRepository,
                                         ClubManagerBindingRepository clubManagerBindingRepository,
                                         ClubDutyRepository clubDutyRepository,
                                         RewardItemTargetClubRepository rewardItemTargetClubRepository,
                                         CurrentUserProvider currentUserProvider,
                                         AuditService auditService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.scoreRecordRepository = scoreRecordRepository;
        this.rewardOrderRepository = rewardOrderRepository;
        this.clubJoinRequestRepository = clubJoinRequestRepository;
        this.registrationRepository = registrationRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.notificationRepository = notificationRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.managerInfoRepository = managerInfoRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.clubDutyRepository = clubDutyRepository;
        this.rewardItemTargetClubRepository = rewardItemTargetClubRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional
    public AdminDirtyDataCleanupResponse cleanDirtyData() {
        AuthenticatedUser admin = requireAdmin();

        int attendanceRecordsRemoved = attendanceRecordRepository.deleteOrphanedRecords();
        int attendanceSessionsRemoved = attendanceSessionRepository.deleteOrphanedSessions();
        int clubManagerBindingsRemoved = clubManagerBindingRepository.deleteOrphanedBindings();
        int clubDutiesRemoved = clubDutyRepository.deleteOrphanedDuties();
        int rewardTargetClubsRemoved = rewardItemTargetClubRepository.deleteOrphanedTargets();
        int clubMembersRemoved = clubStudentMemberRepository.deleteOrphanedMemberships();
        int clubJoinRequestsRemoved = clubJoinRequestRepository.deleteOrphanedJoinRequests();
        int registrationsRemoved = registrationRepository.deleteOrphanedRegistrations();
        int scoreRecordsRemoved = scoreRecordRepository.deleteOrphanedRecords();
        int rewardOrdersRemoved = rewardOrderRepository.deleteOrphanedOrders();
        int notificationsRemoved = notificationRepository.deleteOrphanedNotifications();
        int studentProfilesRemoved = studentInfoRepository.deleteOrphanedStudentProfiles();
        int managerProfilesRemoved = managerInfoRepository.deleteOrphanedManagerProfiles();

        int totalRemoved = attendanceRecordsRemoved
                + attendanceSessionsRemoved
                + scoreRecordsRemoved
                + rewardOrdersRemoved
                + clubJoinRequestsRemoved
                + registrationsRemoved
                + clubMembersRemoved
                + notificationsRemoved
                + studentProfilesRemoved
                + managerProfilesRemoved
                + clubManagerBindingsRemoved
                + clubDutiesRemoved
                + rewardTargetClubsRemoved;

        auditService.create(
                "ADMIN_DIRTY_DATA_CLEANED",
                admin.userId(),
                "SYSTEM",
                "DIRTY_DATA",
                "Administrator cleaned dirty data and removed " + totalRemoved + " records"
        );

        return new AdminDirtyDataCleanupResponse(
                attendanceRecordsRemoved,
                attendanceSessionsRemoved,
                scoreRecordsRemoved,
                rewardOrdersRemoved,
                clubJoinRequestsRemoved,
                registrationsRemoved,
                clubMembersRemoved,
                notificationsRemoved,
                studentProfilesRemoved,
                managerProfilesRemoved,
                clubManagerBindingsRemoved,
                clubDutiesRemoved,
                rewardTargetClubsRemoved,
                totalRemoved
        );
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (!currentUser.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return currentUser;
    }
}
