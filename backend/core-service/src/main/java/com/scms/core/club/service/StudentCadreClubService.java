package com.scms.core.club.service;

import com.scms.core.activity.repository.ActivityRepository;
import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubDutyPermission;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubJoinRequestStatus;
import com.scms.core.club.domain.ClubMemberRole;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.ManagerClubMemberResponse;
import com.scms.core.club.dto.StudentCadreClubWorkspaceResponse;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StudentCadreClubService {

    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final ClubDutyAccessService clubDutyAccessService;
    private final ClubDutySupport clubDutySupport;

    public StudentCadreClubService(ClubRepository clubRepository,
                                   ClubStudentMemberRepository clubStudentMemberRepository,
                                   ClubJoinRequestRepository clubJoinRequestRepository,
                                   ActivityRepository activityRepository,
                                   UserRepository userRepository,
                                   StudentInfoRepository studentInfoRepository,
                                   ClubDutyAccessService clubDutyAccessService,
                                   ClubDutySupport clubDutySupport) {
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.clubJoinRequestRepository = clubJoinRequestRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.clubDutyAccessService = clubDutyAccessService;
        this.clubDutySupport = clubDutySupport;
    }

    @Transactional(readOnly = true)
    public StudentCadreClubWorkspaceResponse getWorkspace(Long clubId) {
        ClubStudentMemberEntity membership = requireDutyWorkspaceAccess(clubId);
        ClubEntity club = getRequiredClub(clubId);
        ClubDutyEntity duty = clubDutyAccessService.getDuty(membership);
        return new StudentCadreClubWorkspaceResponse(
                club.getId(),
                club.getName(),
                defaultString(club.getType()),
                defaultString(club.getDescription()),
                club.getStatus().name(),
                clubStudentMemberRepository.findAllByClubId(clubId).size(),
                clubJoinRequestRepository.countByClubIdAndStatus(clubId, ClubJoinRequestStatus.PENDING),
                clubJoinRequestRepository.findAllByClubIdOrderByCreatedAtDescIdDesc(clubId).size(),
                activityRepository.findAllByClubIdInOrderByCreatedAtDescIdDesc(List.of(clubId)).size(),
                duty == null ? null : duty.getId(),
                duty == null ? "" : duty.getName(),
                clubDutySupport.toPermissionValues(duty),
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ManagerClubMemberResponse> listMembers(Long clubId) {
        clubDutyAccessService.requirePermission(clubId, ClubDutyPermission.SCORE_MANAGEMENT);
        List<ClubStudentMemberEntity> members = clubStudentMemberRepository.findAllByClubId(clubId);
        List<Long> studentIds = members.stream().map(ClubStudentMemberEntity::getStudentUserId).distinct().toList();
        Map<Long, UserEntity> userMap = userRepository.findAllByIdInAndRole(studentIds, UserRole.STUDENT)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, item -> item));
        Map<Long, StudentInfoEntity> infoMap = studentInfoRepository.findAllByUserIdIn(studentIds)
                .stream()
                .collect(Collectors.toMap(StudentInfoEntity::getUserId, item -> item));
        Map<Long, ClubDutyEntity> dutyMap = clubDutySupport.loadDutyMapByIds(
                members.stream().map(ClubStudentMemberEntity::getDutyId).filter(Objects::nonNull).distinct().toList()
        );

        return members.stream()
                .sorted(Comparator
                        .comparing(
                                (ClubStudentMemberEntity item) -> {
                                    ClubDutyEntity duty = dutyMap.get(item.getDutyId());
                                    return duty == null ? "\uffff" : duty.getName();
                                },
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(ClubStudentMemberEntity::getStudentUserId))
                .map(member -> {
                    UserEntity user = userMap.get(member.getStudentUserId());
                    StudentInfoEntity info = infoMap.get(member.getStudentUserId());
                    ClubDutyEntity duty = dutyMap.get(member.getDutyId());
                    return new ManagerClubMemberResponse(
                            member.getStudentUserId(),
                            user == null ? "" : user.getUsername(),
                            info != null && trimToNull(info.getDisplayName()) != null ? info.getDisplayName() : "",
                            info == null ? "" : defaultString(info.getStudentNo()),
                            info == null ? "" : defaultString(info.getGrade()),
                            info == null ? "" : defaultString(info.getClassName()),
                            member.getRole() == null ? ClubMemberRole.MEMBER.name() : member.getRole().name(),
                            duty == null ? null : duty.getId(),
                            duty == null ? "" : duty.getName(),
                            clubDutySupport.toPermissionValues(duty),
                            member.getCreatedAt()
                    );
                })
                .toList();
    }

    private ClubStudentMemberEntity requireDutyWorkspaceAccess(Long clubId) {
        ClubStudentMemberEntity membership = clubDutyAccessService.requireMembership(clubId);
        ClubDutyEntity duty = clubDutyAccessService.getDuty(membership);
        if (duty == null || duty.getPermissions() == null || duty.getPermissions().isEmpty()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student duty workspace unavailable");
        }
        return membership;
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
