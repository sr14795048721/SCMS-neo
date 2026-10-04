package com.scms.core.club.service;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubDutyPermission;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubManagerBindingEntity;
import com.scms.core.club.dto.ClubDutyMutationRequest;
import com.scms.core.club.dto.ClubDutyResponse;
import com.scms.core.club.repository.ClubDutyRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ClubDutyManagementService {

    private final ClubDutyRepository clubDutyRepository;
    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final ClubDutySupport clubDutySupport;

    public ClubDutyManagementService(ClubDutyRepository clubDutyRepository,
                                     ClubRepository clubRepository,
                                     ClubStudentMemberRepository clubStudentMemberRepository,
                                     ClubManagerBindingRepository clubManagerBindingRepository,
                                     CurrentUserProvider currentUserProvider,
                                     AuditService auditService,
                                     ClubDutySupport clubDutySupport) {
        this.clubDutyRepository = clubDutyRepository;
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.clubDutySupport = clubDutySupport;
    }

    @Transactional(readOnly = true)
    public List<ClubDutyResponse> listAsAdmin(Long clubId) {
        requireAdmin();
        getRequiredClub(clubId);
        return clubDutySupport.toResponses(clubDutyRepository.findAllByClubIdOrderByCreatedAtAscIdAsc(clubId));
    }

    @Transactional(readOnly = true)
    public List<ClubDutyResponse> listAsManager(Long clubId) {
        requireManagerAccess(clubId);
        getRequiredClub(clubId);
        return clubDutySupport.toResponses(clubDutyRepository.findAllByClubIdOrderByCreatedAtAscIdAsc(clubId));
    }

    @Transactional
    public ClubDutyResponse createAsAdmin(Long clubId, ClubDutyMutationRequest request) {
        AuthenticatedUser actor = requireAdmin();
        ClubDutyEntity duty = createDuty(clubId, request);
        auditService.create("ADMIN_CLUB_DUTY_CREATED", actor.userId(), "CLUB_DUTY", String.valueOf(duty.getId()), "Administrator created a club duty");
        return clubDutySupport.toResponse(duty);
    }

    @Transactional
    public ClubDutyResponse createAsManager(Long clubId, ClubDutyMutationRequest request) {
        AuthenticatedUser actor = requireManagerAccess(clubId);
        ClubDutyEntity duty = createDuty(clubId, request);
        auditService.create("MANAGER_CLUB_DUTY_CREATED", actor.userId(), "CLUB_DUTY", String.valueOf(duty.getId()), "Club manager created a club duty");
        return clubDutySupport.toResponse(duty);
    }

    @Transactional
    public ClubDutyResponse updateAsAdmin(Long clubId, Long dutyId, ClubDutyMutationRequest request) {
        AuthenticatedUser actor = requireAdmin();
        ClubDutyEntity duty = updateDuty(clubId, dutyId, request);
        auditService.create("ADMIN_CLUB_DUTY_UPDATED", actor.userId(), "CLUB_DUTY", String.valueOf(duty.getId()), "Administrator updated a club duty");
        return clubDutySupport.toResponse(duty);
    }

    @Transactional
    public ClubDutyResponse updateAsManager(Long clubId, Long dutyId, ClubDutyMutationRequest request) {
        AuthenticatedUser actor = requireManagerAccess(clubId);
        ClubDutyEntity duty = updateDuty(clubId, dutyId, request);
        auditService.create("MANAGER_CLUB_DUTY_UPDATED", actor.userId(), "CLUB_DUTY", String.valueOf(duty.getId()), "Club manager updated a club duty");
        return clubDutySupport.toResponse(duty);
    }

    @Transactional
    public void deleteAsAdmin(Long clubId, Long dutyId) {
        AuthenticatedUser actor = requireAdmin();
        deleteDuty(clubId, dutyId);
        auditService.create("ADMIN_CLUB_DUTY_DELETED", actor.userId(), "CLUB_DUTY", String.valueOf(dutyId), "Administrator deleted a club duty");
    }

    @Transactional
    public void deleteAsManager(Long clubId, Long dutyId) {
        AuthenticatedUser actor = requireManagerAccess(clubId);
        deleteDuty(clubId, dutyId);
        auditService.create("MANAGER_CLUB_DUTY_DELETED", actor.userId(), "CLUB_DUTY", String.valueOf(dutyId), "Club manager deleted a club duty");
    }

    @Transactional
    public void assignMemberDutyAsAdmin(Long clubId, Long studentUserId, Long dutyId) {
        AuthenticatedUser actor = requireAdmin();
        assignMemberDuty(clubId, studentUserId, dutyId);
        auditService.create("ADMIN_CLUB_MEMBER_DUTY_UPDATED", actor.userId(), "CLUB_MEMBER", String.valueOf(studentUserId), "Administrator updated a club member duty");
    }

    @Transactional
    public void assignMemberDutyAsManager(Long clubId, Long studentUserId, Long dutyId) {
        AuthenticatedUser actor = requireManagerAccess(clubId);
        assignMemberDuty(clubId, studentUserId, dutyId);
        auditService.create("MANAGER_CLUB_MEMBER_DUTY_UPDATED", actor.userId(), "CLUB_MEMBER", String.valueOf(studentUserId), "Club manager updated a club member duty");
    }

    private ClubDutyEntity createDuty(Long clubId, ClubDutyMutationRequest request) {
        getRequiredClub(clubId);
        String name = normalizeName(request.name());
        if (clubDutyRepository.existsByClubIdAndNameIgnoreCase(clubId, name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "club duty name already exists");
        }

        ClubDutyEntity entity = new ClubDutyEntity();
        entity.setClubId(clubId);
        entity.setName(name);
        entity.setPermissions(parsePermissions(request.permissions()));
        return clubDutyRepository.save(entity);
    }

    private ClubDutyEntity updateDuty(Long clubId, Long dutyId, ClubDutyMutationRequest request) {
        getRequiredClub(clubId);
        ClubDutyEntity entity = getRequiredDuty(clubId, dutyId);
        String name = normalizeName(request.name());
        if (clubDutyRepository.existsByClubIdAndNameIgnoreCaseAndIdNot(clubId, name, dutyId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "club duty name already exists");
        }
        entity.setName(name);
        entity.setPermissions(parsePermissions(request.permissions()));
        return clubDutyRepository.save(entity);
    }

    private void deleteDuty(Long clubId, Long dutyId) {
        getRequiredClub(clubId);
        ClubDutyEntity entity = getRequiredDuty(clubId, dutyId);
        var affectedMembers = clubStudentMemberRepository.findAllByClubIdAndDutyId(clubId, dutyId);
        if (!affectedMembers.isEmpty()) {
            affectedMembers.forEach(member -> member.setDutyId(null));
            clubStudentMemberRepository.saveAll(affectedMembers);
        }
        clubDutyRepository.delete(entity);
    }

    private void assignMemberDuty(Long clubId, Long studentUserId, Long dutyId) {
        getRequiredClub(clubId);
        var member = clubStudentMemberRepository.findByClubIdAndStudentUserId(clubId, studentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club member not found"));
        if (dutyId == null || dutyId <= 0) {
            member.setDutyId(null);
            clubStudentMemberRepository.save(member);
            return;
        }
        ClubDutyEntity duty = getRequiredDuty(clubId, dutyId);
        member.setDutyId(duty.getId());
        clubStudentMemberRepository.save(member);
    }

    private ClubDutyEntity getRequiredDuty(Long clubId, Long dutyId) {
        return clubDutyRepository.findByIdAndClubId(dutyId, clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club duty not found"));
    }

    private ClubEntity getRequiredClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club not found"));
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (!user.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return user;
    }

    private AuthenticatedUser requireManagerAccess(Long clubId) {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager role required");
        }
        if (!clubManagerBindingRepository.existsByClubIdAndManagerUserId(clubId, user.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "club manager permission denied");
        }
        return user;
    }

    private String normalizeName(String value) {
        String normalized = clubDutySupport.normalizeDutyName(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club duty name is required");
        }
        return normalized;
    }

    private Set<ClubDutyPermission> parsePermissions(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<ClubDutyPermission> permissions = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = clubDutySupport.normalizeDutyName(value);
            if (normalized == null) {
                continue;
            }
            try {
                permissions.add(ClubDutyPermission.valueOf(normalized.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club duty permission is invalid");
            }
        }
        return permissions;
    }
}
