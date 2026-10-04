package com.scms.core.club.service;

import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubDutyPermission;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubDutyRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.stereotype.Service;

@Service
public class ClubDutyAccessService {

    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubDutyRepository clubDutyRepository;
    private final CurrentUserProvider currentUserProvider;

    public ClubDutyAccessService(ClubStudentMemberRepository clubStudentMemberRepository,
                                 ClubDutyRepository clubDutyRepository,
                                 CurrentUserProvider currentUserProvider) {
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.clubDutyRepository = clubDutyRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public AuthenticatedUser requireStudent() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return user;
    }

    public ClubStudentMemberEntity requireMembership(Long clubId) {
        AuthenticatedUser student = requireStudent();
        return clubStudentMemberRepository.findByClubIdAndStudentUserId(clubId, student.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "student club membership required"));
    }

    public ClubStudentMemberEntity requirePermission(Long clubId, ClubDutyPermission permission) {
        ClubStudentMemberEntity membership = requireMembership(clubId);
        if (!hasPermission(membership, permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student duty permission denied");
        }
        return membership;
    }

    public boolean hasPermission(ClubStudentMemberEntity membership, ClubDutyPermission permission) {
        if (membership == null || membership.getDutyId() == null || permission == null) {
            return false;
        }
        ClubDutyEntity duty = clubDutyRepository.findById(membership.getDutyId()).orElse(null);
        return duty != null && duty.getPermissions().contains(permission);
    }

    public ClubDutyEntity getDuty(ClubStudentMemberEntity membership) {
        if (membership == null || membership.getDutyId() == null) {
            return null;
        }
        return clubDutyRepository.findById(membership.getDutyId()).orElse(null);
    }
}
