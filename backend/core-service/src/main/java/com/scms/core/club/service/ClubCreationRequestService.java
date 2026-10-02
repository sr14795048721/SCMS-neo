package com.scms.core.club.service;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubCreationRequestEntity;
import com.scms.core.club.domain.ClubCreationRequestStatus;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubManagerBindingEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.dto.ClubCreationRequestResponse;
import com.scms.core.club.dto.CreateClubCreationRequestRequest;
import com.scms.core.club.repository.ClubCreationRequestRepository;
import com.scms.core.club.repository.ClubManagerBindingRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ClubCreationRequestService {

    private static final int MAX_CLUB_NAME_LENGTH = 12;

    private final ClubCreationRequestRepository clubCreationRequestRepository;
    private final ClubRepository clubRepository;
    private final ClubManagerBindingRepository clubManagerBindingRepository;
    private final UserRepository userRepository;
    private final ManagerInfoRepository managerInfoRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ClubCreationRequestService(ClubCreationRequestRepository clubCreationRequestRepository,
                                      ClubRepository clubRepository,
                                      ClubManagerBindingRepository clubManagerBindingRepository,
                                      UserRepository userRepository,
                                      ManagerInfoRepository managerInfoRepository,
                                      CurrentUserProvider currentUserProvider,
                                      AuditService auditService,
                                      NotificationService notificationService) {
        this.clubCreationRequestRepository = clubCreationRequestRepository;
        this.clubRepository = clubRepository;
        this.clubManagerBindingRepository = clubManagerBindingRepository;
        this.userRepository = userRepository;
        this.managerInfoRepository = managerInfoRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ClubCreationRequestResponse> listManagerRequests() {
        AuthenticatedUser manager = requireManager();
        return buildResponses(
                clubCreationRequestRepository.findAllByApplicantManagerUserIdOrderByCreatedAtDescIdDesc(manager.userId())
        );
    }

    @Transactional
    public ClubCreationRequestResponse createManagerRequest(CreateClubCreationRequestRequest request) {
        AuthenticatedUser manager = requireManager();
        if (clubCreationRequestRepository.existsByApplicantManagerUserIdAndStatus(manager.userId(), ClubCreationRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CONFLICT, "club creation request is already pending");
        }

        String name = normalizeName(request.name());
        String type = normalizeType(request.type());
        String description = trimToNull(request.description());
        String applyReason = normalizeApplyReason(request.applyReason());

        if (clubRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "club name already exists");
        }

        ClubCreationRequestEntity entity = new ClubCreationRequestEntity();
        entity.setApplicantManagerUserId(manager.userId());
        entity.setName(name);
        entity.setType(type);
        entity.setDescription(description);
        entity.setApplyReason(applyReason);
        entity.setStatus(ClubCreationRequestStatus.PENDING);
        ClubCreationRequestEntity saved = clubCreationRequestRepository.save(entity);

        auditService.create(
                "MANAGER_CLUB_CREATION_REQUEST_CREATED",
                manager.userId(),
                "CLUB_CREATION_REQUEST",
                String.valueOf(saved.getId()),
                "Club manager submitted a club creation request"
        );
        notifyAdminsForNewRequest(saved);
        return buildResponses(List.of(saved)).getFirst();
    }

    @Transactional(readOnly = true)
    public List<ClubCreationRequestResponse> listAdminRequests() {
        requireAdmin();
        return buildResponses(clubCreationRequestRepository.findAllByOrderByCreatedAtDescIdDesc());
    }

    @Transactional
    public ClubCreationRequestResponse approveRequest(Long requestId) {
        AuthenticatedUser admin = requireAdmin();
        ClubCreationRequestEntity request = getRequiredRequest(requestId);
        if (request.getStatus() == ClubCreationRequestStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "rejected request cannot be approved");
        }
        if (request.getStatus() == ClubCreationRequestStatus.APPROVED) {
            return buildResponses(List.of(request)).getFirst();
        }
        if (clubRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException(ErrorCode.CONFLICT, "club name already exists");
        }

        ClubEntity club = new ClubEntity();
        club.setName(request.getName());
        club.setType(request.getType());
        club.setDescription(request.getDescription());
        club.setStatus(ClubStatus.ACTIVE);
        club.setCreatedBy(admin.userId());
        ClubEntity savedClub = clubRepository.save(club);

        ClubManagerBindingEntity binding = new ClubManagerBindingEntity();
        binding.setClubId(savedClub.getId());
        binding.setManagerUserId(request.getApplicantManagerUserId());
        clubManagerBindingRepository.save(binding);

        request.setStatus(ClubCreationRequestStatus.APPROVED);
        request.setReviewedBy(admin.userId());
        request.setReviewedAt(Instant.now());
        ClubCreationRequestEntity savedRequest = clubCreationRequestRepository.save(request);

        auditService.create(
                "ADMIN_CLUB_CREATION_REQUEST_APPROVED",
                admin.userId(),
                "CLUB_CREATION_REQUEST",
                String.valueOf(savedRequest.getId()),
                "Administrator approved a club creation request"
        );
        notificationService.create(
                savedRequest.getApplicantManagerUserId(),
                "CLUB_CREATION_REQUEST",
                "club creation approved",
                "Your club creation request has been approved.",
                "/club-admin/clubs/" + savedClub.getId()
        );
        return buildResponses(List.of(savedRequest)).getFirst();
    }

    @Transactional
    public ClubCreationRequestResponse rejectRequest(Long requestId) {
        AuthenticatedUser admin = requireAdmin();
        ClubCreationRequestEntity request = getRequiredRequest(requestId);
        if (request.getStatus() == ClubCreationRequestStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "approved request cannot be rejected");
        }
        if (request.getStatus() == ClubCreationRequestStatus.REJECTED) {
            return buildResponses(List.of(request)).getFirst();
        }

        request.setStatus(ClubCreationRequestStatus.REJECTED);
        request.setReviewedBy(admin.userId());
        request.setReviewedAt(Instant.now());
        ClubCreationRequestEntity savedRequest = clubCreationRequestRepository.save(request);

        auditService.create(
                "ADMIN_CLUB_CREATION_REQUEST_REJECTED",
                admin.userId(),
                "CLUB_CREATION_REQUEST",
                String.valueOf(savedRequest.getId()),
                "Administrator rejected a club creation request"
        );
        notificationService.create(
                savedRequest.getApplicantManagerUserId(),
                "CLUB_CREATION_REQUEST",
                "club creation rejected",
                "Your club creation request was not approved.",
                "/club-admin/create-club-apply"
        );
        return buildResponses(List.of(savedRequest)).getFirst();
    }

    private List<ClubCreationRequestResponse> buildResponses(List<ClubCreationRequestEntity> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> applicantIds = requests.stream()
                .map(ClubCreationRequestEntity::getApplicantManagerUserId)
                .distinct()
                .toList();
        List<Long> reviewerIds = requests.stream()
                .map(ClubCreationRequestEntity::getReviewedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserEntity> applicantUserMap = userRepository.findAllByIdInAndRole(applicantIds, UserRole.CLUB_MANAGER)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, item -> item));
        Map<Long, ManagerInfoEntity> applicantInfoMap = managerInfoRepository.findAllByUserIdIn(applicantIds)
                .stream()
                .collect(Collectors.toMap(ManagerInfoEntity::getUserId, item -> item));
        Map<Long, UserEntity> reviewerUserMap = loadUserMap(reviewerIds);

        return requests.stream()
                .map(request -> {
                    UserEntity applicantUser = applicantUserMap.get(request.getApplicantManagerUserId());
                    ManagerInfoEntity applicantInfo = applicantInfoMap.get(request.getApplicantManagerUserId());
                    UserEntity reviewerUser = request.getReviewedBy() == null ? null : reviewerUserMap.get(request.getReviewedBy());
                    return new ClubCreationRequestResponse(
                            request.getId(),
                            request.getApplicantManagerUserId(),
                            resolveManagerName(applicantUser, applicantInfo),
                            applicantInfo == null ? "" : defaultString(applicantInfo.getManagerNo()),
                            request.getName(),
                            request.getType(),
                            defaultString(request.getDescription()),
                            request.getApplyReason(),
                            request.getStatus().name(),
                            request.getReviewedBy(),
                            reviewerUser == null ? "" : defaultString(reviewerUser.getUsername()),
                            request.getReviewedAt(),
                            request.getCreatedAt(),
                            request.getUpdatedAt()
                    );
                })
                .toList();
    }

    private Map<Long, UserEntity> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserEntity> result = new LinkedHashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            result.put(user.getId(), user);
        }
        return result;
    }

    private void notifyAdminsForNewRequest(ClubCreationRequestEntity request) {
        String message = "A club manager submitted a new club creation request: " + request.getName();
        for (UserEntity admin : userRepository.findAllByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN))) {
            notificationService.create(
                    admin.getId(),
                    "CLUB_CREATION_REQUEST",
                    "new club creation request",
                    message,
                    "/sys-admin/club-manage?tab=requests"
            );
        }
    }

    private ClubCreationRequestEntity getRequiredRequest(Long requestId) {
        return clubCreationRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "club creation request not found"));
    }

    private String resolveManagerName(UserEntity user, ManagerInfoEntity info) {
        if (info != null && info.getDisplayName() != null && !info.getDisplayName().isBlank()) {
            return info.getDisplayName();
        }
        return user == null ? "" : defaultString(user.getUsername());
    }

    private String normalizeName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club name is required");
        }
        if (normalized.length() > MAX_CLUB_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club name too long");
        }
        return normalized;
    }

    private String normalizeType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club type is required");
        }
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64).trim();
        }
        return normalized;
    }

    private String normalizeApplyReason(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "apply reason is required");
        }
        if (normalized.length() > 2000) {
            normalized = normalized.substring(0, 2000).trim();
        }
        return normalized;
    }

    private String trimToNull(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private AuthenticatedUser requireManager() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager role required");
        }
        return user;
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (!user.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return user;
    }
}
