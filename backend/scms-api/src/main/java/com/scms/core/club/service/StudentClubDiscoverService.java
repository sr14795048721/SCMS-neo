package com.scms.core.club.service;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubJoinRequestEntity;
import com.scms.core.club.domain.ClubJoinRequestStatus;
import com.scms.core.club.domain.ClubMemberRole;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.StudentDiscoverClubResponse;
import com.scms.core.club.dto.StudentDiscoverClubsResponse;
import com.scms.core.club.dto.StudentPendingClubJoinRequestResponse;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubRelationCountProjection;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentClubDiscoverService {

    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ClubDutySupport clubDutySupport;

    public StudentClubDiscoverService(ClubRepository clubRepository,
                                      ClubStudentMemberRepository clubStudentMemberRepository,
                                      ClubJoinRequestRepository clubJoinRequestRepository,
                                      CurrentUserProvider currentUserProvider,
                                      ClubDutySupport clubDutySupport) {
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.clubJoinRequestRepository = clubJoinRequestRepository;
        this.currentUserProvider = currentUserProvider;
        this.clubDutySupport = clubDutySupport;
    }

    @Transactional(readOnly = true)
    public StudentDiscoverClubsResponse getDiscoverClubs() {
        AuthenticatedUser student = requireStudent();

        List<ClubStudentMemberEntity> memberships =
                clubStudentMemberRepository.findAllByStudentUserIdOrderByCreatedAtAscIdAsc(student.userId());
        ClubStudentMemberEntity primaryMembership = memberships.stream()
                .filter(item -> item.getClubId() != null)
                .findFirst()
                .orElse(null);

        List<ClubJoinRequestEntity> pendingRequests =
                clubJoinRequestRepository.findAllByStudentUserIdAndStatusOrderByCreatedAtAscIdAsc(
                        student.userId(),
                        ClubJoinRequestStatus.PENDING
                );
        ClubJoinRequestEntity primaryPendingRequest = pendingRequests.stream()
                .filter(item -> item.getClubId() != null)
                .findFirst()
                .orElse(null);

        Set<Long> involvedClubIds = new LinkedHashSet<>();
        memberships.stream().map(ClubStudentMemberEntity::getClubId).filter(Objects::nonNull).forEach(involvedClubIds::add);
        pendingRequests.stream().map(ClubJoinRequestEntity::getClubId).filter(Objects::nonNull).forEach(involvedClubIds::add);

        List<ClubEntity> discoverClubs = clubRepository.findAllByStatusOrderByCreatedAtDescIdDesc(ClubStatus.ACTIVE)
                .stream()
                .filter(club -> !involvedClubIds.contains(club.getId()))
                .toList();

        List<Long> countedClubIds = new java.util.ArrayList<>(involvedClubIds);
        countedClubIds.addAll(discoverClubs.stream().map(ClubEntity::getId).filter(Objects::nonNull).toList());

        Map<Long, ClubEntity> clubMap = clubRepository.findAllById(countedClubIds).stream()
                .collect(Collectors.toMap(ClubEntity::getId, Function.identity(), (left, right) -> left));
        Map<Long, Long> memberCounts = loadMemberCounts(countedClubIds);
        Map<Long, ClubDutyEntity> dutyMap = clubDutySupport.loadDutyMapByIds(
                memberships.stream().map(ClubStudentMemberEntity::getDutyId).filter(Objects::nonNull).distinct().toList()
        );

        StudentDiscoverClubResponse joinedClub = primaryMembership == null
                ? null
                : toDiscoverClubResponse(clubMap.get(primaryMembership.getClubId()), memberCounts, primaryMembership, dutyMap);
        StudentPendingClubJoinRequestResponse pendingRequest = primaryPendingRequest == null
                ? null
                : toPendingRequestResponse(primaryPendingRequest, clubMap.get(primaryPendingRequest.getClubId()), memberCounts);

        List<StudentDiscoverClubResponse> clubs = discoverClubs.stream()
                .map(club -> toDiscoverClubResponse(club, memberCounts, null, dutyMap))
                .toList();

        return new StudentDiscoverClubsResponse(joinedClub, pendingRequest, clubs);
    }

    private StudentDiscoverClubResponse toDiscoverClubResponse(ClubEntity club,
                                                               Map<Long, Long> memberCounts,
                                                               ClubStudentMemberEntity membership,
                                                               Map<Long, ClubDutyEntity> dutyMap) {
        if (club == null) {
            return null;
        }
        ClubMemberRole role = membership == null || membership.getRole() == null ? ClubMemberRole.MEMBER : membership.getRole();
        Long dutyId = membership == null ? null : membership.getDutyId();
        ClubDutyEntity duty = dutyId == null ? null : dutyMap.get(dutyId);
        java.util.List<String> dutyPermissions = clubDutySupport.toPermissionValues(duty);
        return new StudentDiscoverClubResponse(
                club.getId(),
                club.getName(),
                defaultString(club.getType()),
                defaultString(club.getDescription()),
                memberCounts.getOrDefault(club.getId(), 0L),
                membership == null ? "" : role.name(),
                duty == null ? null : duty.getId(),
                duty == null ? "" : duty.getName(),
                dutyPermissions,
                !dutyPermissions.isEmpty(),
                club.getCreatedAt()
        );
    }

    private StudentPendingClubJoinRequestResponse toPendingRequestResponse(ClubJoinRequestEntity request,
                                                                           ClubEntity club,
                                                                           Map<Long, Long> memberCounts) {
        if (request == null || club == null) {
            return null;
        }
        return new StudentPendingClubJoinRequestResponse(
                request.getId(),
                club.getId(),
                club.getName(),
                defaultString(club.getType()),
                defaultString(club.getDescription()),
                memberCounts.getOrDefault(club.getId(), 0L),
                defaultString(request.getReason()),
                request.getStatus().name(),
                request.getCreatedAt()
        );
    }

    private Map<Long, Long> loadMemberCounts(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        return clubStudentMemberRepository.countByClubIds(clubIds).stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }

    private AuthenticatedUser requireStudent() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return user;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
