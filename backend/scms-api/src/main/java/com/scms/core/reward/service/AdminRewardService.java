package com.scms.core.reward.service;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.reward.domain.RewardItemEntity;
import com.scms.core.reward.domain.RewardItemStatus;
import com.scms.core.reward.domain.RewardItemTargetClubEntity;
import com.scms.core.reward.domain.RewardOrderEntity;
import com.scms.core.reward.domain.RewardOrderStatus;
import com.scms.core.reward.domain.RewardVisibilityScope;
import com.scms.core.reward.dto.AdminRewardItemResponse;
import com.scms.core.reward.dto.AdminRewardMutationRequest;
import com.scms.core.reward.dto.AdminRewardOrderResponse;
import com.scms.core.reward.repository.RewardItemRepository;
import com.scms.core.reward.repository.RewardItemTargetClubRepository;
import com.scms.core.reward.repository.RewardOrderRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminRewardService {

    private final RewardItemRepository rewardItemRepository;
    private final RewardItemTargetClubRepository rewardItemTargetClubRepository;
    private final RewardOrderRepository rewardOrderRepository;
    private final RewardImageStorageService rewardImageStorageService;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AdminRewardService(RewardItemRepository rewardItemRepository,
                              RewardItemTargetClubRepository rewardItemTargetClubRepository,
                              RewardOrderRepository rewardOrderRepository,
                              RewardImageStorageService rewardImageStorageService,
                              ClubRepository clubRepository,
                              UserRepository userRepository,
                              StudentInfoRepository studentInfoRepository,
                              CurrentUserProvider currentUserProvider,
                              NotificationService notificationService,
                              AuditService auditService) {
        this.rewardItemRepository = rewardItemRepository;
        this.rewardItemTargetClubRepository = rewardItemTargetClubRepository;
        this.rewardOrderRepository = rewardOrderRepository;
        this.rewardImageStorageService = rewardImageStorageService;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminRewardItemResponse> listAdminRewards() {
        requireAdmin();
        List<RewardItemEntity> rewards = rewardItemRepository.findAllByOrderByCreatedAtDescIdDesc();
        Map<Long, List<Long>> rewardClubIdsMap = loadRewardClubIdsMap(rewards.stream().map(RewardItemEntity::getId).toList());
        Map<Long, List<String>> rewardClubNamesMap = loadRewardClubNamesMap(rewardClubIdsMap);
        return rewards
                .stream()
                .map(entity -> toRewardResponse(
                        entity,
                        rewardClubIdsMap.getOrDefault(entity.getId(), List.of()),
                        rewardClubNamesMap.getOrDefault(entity.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public AdminRewardItemResponse createReward(AdminRewardMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        RewardItemEntity entity = new RewardItemEntity();
        RewardMutation mutation = normalizeMutation(request, true);
        applyMutation(entity, mutation);
        RewardItemEntity saved = rewardItemRepository.save(entity);
        syncRewardTargetClubs(saved.getId(), mutation);
        auditService.create("ADMIN_REWARD_CREATED", admin.userId(), "REWARD", String.valueOf(saved.getId()), "Administrator created a reward item");
        return toRewardResponse(saved, mutation.clubIds(), loadClubNames(mutation.clubIds()));
    }

    @Transactional
    public AdminRewardItemResponse updateReward(Long rewardId, AdminRewardMutationRequest request) {
        AuthenticatedUser admin = requireAdmin();
        RewardItemEntity entity = getRequiredReward(rewardId);
        RewardMutation mutation = normalizeMutation(request, false);
        applyMutation(entity, mutation);
        RewardItemEntity saved = rewardItemRepository.save(entity);
        syncRewardTargetClubs(saved.getId(), mutation);
        auditService.create("ADMIN_REWARD_UPDATED", admin.userId(), "REWARD", String.valueOf(saved.getId()), "Administrator updated a reward item");
        return toRewardResponse(saved, mutation.clubIds(), loadClubNames(mutation.clubIds()));
    }

    @Transactional
    public AdminRewardItemResponse uploadImage(Long rewardId, MultipartFile file) {
        AuthenticatedUser admin = requireAdmin();
        RewardItemEntity entity = getRequiredReward(rewardId);
        String oldImagePath = entity.getImagePath();

        StoredRewardImage storedImage = rewardImageStorageService.store(file);
        entity.setImagePath(storedImage.imagePath());
        entity.setImageContentType(storedImage.contentType());
        RewardItemEntity saved = rewardItemRepository.save(entity);

        if (oldImagePath != null && !Objects.equals(oldImagePath, storedImage.imagePath())) {
            rewardImageStorageService.deleteIfExists(oldImagePath);
        }

        auditService.create("ADMIN_REWARD_IMAGE_UPLOADED", admin.userId(), "REWARD", String.valueOf(saved.getId()), "Administrator uploaded a reward image");
        return toRewardResponse(saved);
    }

    @Transactional(readOnly = true)
    public RewardImageContent resolveImageContent(Long rewardId) {
        requireAdmin();
        RewardItemEntity entity = getRequiredReward(rewardId);
        if (entity.getImagePath() == null || entity.getImagePath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "reward image not found");
        }
        return rewardImageStorageService.resolveContent(entity.getImagePath(), entity.getImageContentType());
    }

    @Transactional
    public void deleteReward(Long rewardId) {
        AuthenticatedUser admin = requireAdmin();
        RewardItemEntity entity = getRequiredReward(rewardId);
        if (rewardOrderRepository.countByRewardId(rewardId) > 0) {
            throw new BusinessException(ErrorCode.DEPENDENCY_EXISTS, "reward item is still referenced by reward orders");
        }

        String imagePath = entity.getImagePath();
        rewardItemTargetClubRepository.deleteAllByRewardId(rewardId);
        rewardItemRepository.delete(entity);
        rewardImageStorageService.deleteIfExists(imagePath);
        auditService.create("ADMIN_REWARD_DELETED", admin.userId(), "REWARD", String.valueOf(rewardId), "Administrator deleted a reward item");
    }

    @Transactional(readOnly = true)
    public List<AdminRewardOrderResponse> listAdminOrders() {
        requireAdmin();
        List<RewardOrderEntity> orders = rewardOrderRepository.findAllByOrderByCreatedAtDescIdDesc();

        Set<Long> rewardIds = orders.stream()
                .map(RewardOrderEntity::getRewardId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> userIds = orders.stream()
                .map(RewardOrderEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> completedByIds = orders.stream()
                .map(RewardOrderEntity::getCompletedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> rejectedByIds = orders.stream()
                .map(RewardOrderEntity::getRejectedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, RewardItemEntity> rewardMap = rewardItemRepository.findAllById(rewardIds)
                .stream()
                .collect(Collectors.toMap(RewardItemEntity::getId, item -> item));
        Map<Long, UserEntity> userMap = loadUserMap(userIds);
        Map<Long, UserEntity> completedByMap = loadUserMap(completedByIds);
        Map<Long, UserEntity> rejectedByMap = loadUserMap(rejectedByIds);
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(userIds);

        return orders.stream()
                .map(order -> toOrderResponse(order, rewardMap, userMap, studentInfoMap, completedByMap, rejectedByMap))
                .toList();
    }

    @Transactional
    public AdminRewardOrderResponse completeOrder(Long orderId) {
        AuthenticatedUser admin = requireAdmin();
        RewardOrderEntity entity = getRequiredOrderForUpdate(orderId);
        if (entity.getStatus() != RewardOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "only pending reward orders can be completed");
        }
        entity.setStatus(RewardOrderStatus.COMPLETED);
        entity.setCompletedBy(admin.userId());
        entity.setCompletedAt(Instant.now());
        entity.setRejectedBy(null);
        entity.setRejectedAt(null);
        rewardOrderRepository.save(entity);
        auditService.create("ADMIN_REWARD_ORDER_COMPLETED", admin.userId(), "REWARD_ORDER", String.valueOf(orderId), "Administrator completed a reward order");
        notifyStudentOrderCompleted(entity);
        return buildSingleOrderResponse(entity);
    }

    @Transactional
    public AdminRewardOrderResponse rejectOrder(Long orderId) {
        AuthenticatedUser admin = requireAdmin();
        RewardOrderEntity entity = getRequiredOrderForUpdate(orderId);
        if (entity.getStatus() != RewardOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "only pending reward orders can be rejected");
        }
        entity.setStatus(RewardOrderStatus.REJECTED);
        entity.setRejectedBy(admin.userId());
        entity.setRejectedAt(Instant.now());
        entity.setCompletedBy(null);
        entity.setCompletedAt(null);
        rewardOrderRepository.save(entity);
        rewardItemRepository.increaseStock(entity.getRewardId(), Instant.now());
        auditService.create("ADMIN_REWARD_ORDER_REJECTED", admin.userId(), "REWARD_ORDER", String.valueOf(orderId), "Administrator rejected a reward order");
        notifyStudentOrderRejected(entity);
        return buildSingleOrderResponse(entity);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        AuthenticatedUser admin = requireAdmin();
        RewardOrderEntity entity = getRequiredOrderForUpdate(orderId);
        if (entity.getStatus() != RewardOrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONFLICT, "only completed reward orders can be deleted");
        }
        rewardOrderRepository.delete(entity);
        auditService.create("ADMIN_REWARD_ORDER_DELETED", admin.userId(), "REWARD_ORDER", String.valueOf(orderId), "Administrator deleted a reward order");
    }

    private AdminRewardOrderResponse buildSingleOrderResponse(RewardOrderEntity entity) {
        Map<Long, RewardItemEntity> rewardMap = rewardItemRepository.findAllById(List.of(entity.getRewardId()))
                .stream()
                .collect(Collectors.toMap(RewardItemEntity::getId, item -> item));
        Map<Long, UserEntity> userMap = loadUserMap(List.of(entity.getUserId()));
        Map<Long, StudentInfoEntity> studentInfoMap = loadStudentInfoMap(List.of(entity.getUserId()));
        Map<Long, UserEntity> completedByMap = entity.getCompletedBy() == null
                ? Map.of()
                : loadUserMap(List.of(entity.getCompletedBy()));
        Map<Long, UserEntity> rejectedByMap = entity.getRejectedBy() == null
                ? Map.of()
                : loadUserMap(List.of(entity.getRejectedBy()));
        return toOrderResponse(entity, rewardMap, userMap, studentInfoMap, completedByMap, rejectedByMap);
    }

    private AdminRewardOrderResponse toOrderResponse(RewardOrderEntity entity,
                                                     Map<Long, RewardItemEntity> rewardMap,
                                                     Map<Long, UserEntity> userMap,
                                                     Map<Long, StudentInfoEntity> studentInfoMap,
                                                     Map<Long, UserEntity> completedByMap,
                                                     Map<Long, UserEntity> rejectedByMap) {
        UserEntity user = userMap.get(entity.getUserId());
        StudentInfoEntity studentInfo = studentInfoMap.get(entity.getUserId());
        UserEntity completedByUser = entity.getCompletedBy() == null ? null : completedByMap.get(entity.getCompletedBy());
        UserEntity rejectedByUser = entity.getRejectedBy() == null ? null : rejectedByMap.get(entity.getRejectedBy());
        RewardItemEntity reward = rewardMap.get(entity.getRewardId());

        return new AdminRewardOrderResponse(
                entity.getId(),
                entity.getRewardId(),
                reward == null ? "" : defaultString(reward.getName()),
                entity.getUserId(),
                user == null ? "" : defaultString(user.getUsername()),
                resolveStudentDisplayName(user, studentInfo),
                studentInfo == null ? "" : defaultString(studentInfo.getStudentNo()),
                entity.getScoreCost(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getCompletedBy(),
                completedByUser == null ? "" : defaultString(completedByUser.getUsername()),
                entity.getRejectedAt(),
                entity.getRejectedBy(),
                rejectedByUser == null ? "" : defaultString(rejectedByUser.getUsername())
        );
    }

    private void notifyStudentOrderCompleted(RewardOrderEntity entity) {
        notificationService.create(
                entity.getUserId(),
                "REWARD_ORDER",
                "reward order completed",
                "Your reward redemption request has been completed.",
                "/student/score-rewards"
        );
    }

    private void notifyStudentOrderRejected(RewardOrderEntity entity) {
        notificationService.create(
                entity.getUserId(),
                "REWARD_ORDER",
                "reward order rejected",
                "Your reward redemption request was rejected and the score has been returned.",
                "/student/score-rewards"
        );
    }

    private AdminRewardItemResponse toRewardResponse(RewardItemEntity entity,
                                                     List<Long> clubIds,
                                                     List<String> clubNames) {
        return new AdminRewardItemResponse(
                entity.getId(),
                entity.getName(),
                entity.getScoreCost(),
                entity.getStock(),
                entity.getStatus().name(),
                entity.getVisibilityScope().name(),
                clubIds,
                clubNames,
                entity.getImagePath() != null && !entity.getImagePath().isBlank(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AdminRewardItemResponse toRewardResponse(RewardItemEntity entity) {
        List<Long> clubIds = loadRewardClubIdsMap(List.of(entity.getId())).getOrDefault(entity.getId(), List.of());
        return toRewardResponse(entity, clubIds, loadClubNames(clubIds));
    }

    private void applyMutation(RewardItemEntity entity, RewardMutation mutation) {
        entity.setName(mutation.name());
        entity.setScoreCost(mutation.scoreCost());
        entity.setStock(mutation.stock());
        entity.setStatus(mutation.status());
        entity.setVisibilityScope(mutation.visibilityScope());
    }

    private RewardMutation normalizeMutation(AdminRewardMutationRequest request, boolean creating) {
        String name = normalizeName(request.name());
        int scoreCost = normalizeNonNegative(request.scoreCost(), "score cost is required");
        int stock = normalizeNonNegative(request.stock(), "stock is required");
        RewardItemStatus status = parseStatus(request.status());
        RewardVisibilityScope visibilityScope = parseVisibilityScope(request.visibilityScope());
        List<Long> clubIds = normalizeClubIds(request.clubIds());

        if (creating && visibilityScope == RewardVisibilityScope.UNASSIGNED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "unassigned rewards cannot be created directly");
        }
        if (visibilityScope == RewardVisibilityScope.GLOBAL || visibilityScope == RewardVisibilityScope.UNASSIGNED) {
            if (!clubIds.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "global or unassigned rewards cannot bind clubs");
            }
        }
        if (visibilityScope == RewardVisibilityScope.CLUB) {
            if (clubIds.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "club rewards must bind at least one club");
            }
            validateTargetClubs(clubIds);
        }

        return new RewardMutation(name, scoreCost, stock, status, visibilityScope, clubIds);
    }

    private int normalizeNonNegative(Integer value, String message) {
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        if (value < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "value must be greater than or equal to 0");
        }
        return value;
    }

    private String normalizeName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reward name is required");
        }
        return normalized;
    }

    private RewardItemStatus parseStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reward status is required");
        }
        try {
            return RewardItemStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reward status is invalid");
        }
    }

    private RewardVisibilityScope parseVisibilityScope(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reward visibility scope is required");
        }
        try {
            return RewardVisibilityScope.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reward visibility scope is invalid");
        }
    }

    private RewardItemEntity getRequiredReward(Long rewardId) {
        return rewardItemRepository.findById(rewardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reward item not found"));
    }

    private RewardOrderEntity getRequiredOrder(Long orderId) {
        return rewardOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reward order not found"));
    }

    private RewardOrderEntity getRequiredOrderForUpdate(Long orderId) {
        return rewardOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reward order not found"));
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (!currentUser.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return currentUser;
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

    private Map<Long, StudentInfoEntity> loadStudentInfoMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return studentInfoRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(StudentInfoEntity::getUserId, item -> item));
    }

    private String resolveStudentDisplayName(UserEntity user, StudentInfoEntity studentInfo) {
        if (studentInfo != null && trimToNull(studentInfo.getDisplayName()) != null) {
            return studentInfo.getDisplayName();
        }
        return user == null ? "" : defaultString(user.getUsername());
    }

    private List<Long> normalizeClubIds(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(Long::longValue)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private void validateTargetClubs(List<Long> clubIds) {
        Map<Long, ClubEntity> clubMap = clubRepository.findAllById(clubIds).stream()
                .collect(Collectors.toMap(ClubEntity::getId, item -> item));
        if (clubMap.size() != clubIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reward target clubs contain invalid clubs");
        }
        boolean hasInactive = clubMap.values().stream().anyMatch(club -> club.getStatus() != ClubStatus.ACTIVE);
        if (hasInactive) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reward target clubs must all be active");
        }
    }

    private void syncRewardTargetClubs(Long rewardId, RewardMutation mutation) {
        rewardItemTargetClubRepository.deleteAllByRewardId(rewardId);
        if (mutation.visibilityScope() != RewardVisibilityScope.CLUB || mutation.clubIds().isEmpty()) {
            return;
        }
        List<RewardItemTargetClubEntity> targets = mutation.clubIds().stream()
                .map(clubId -> {
                    RewardItemTargetClubEntity entity = new RewardItemTargetClubEntity();
                    entity.setRewardId(rewardId);
                    entity.setClubId(clubId);
                    return entity;
                })
                .toList();
        rewardItemTargetClubRepository.saveAll(targets);
    }

    private Map<Long, List<Long>> loadRewardClubIdsMap(Collection<Long> rewardIds) {
        if (rewardIds == null || rewardIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (RewardItemTargetClubEntity target : rewardItemTargetClubRepository.findAllByRewardIdIn(rewardIds)) {
            result.computeIfAbsent(target.getRewardId(), ignored -> new java.util.ArrayList<>()).add(target.getClubId());
        }
        return result;
    }

    private Map<Long, List<String>> loadRewardClubNamesMap(Map<Long, List<Long>> rewardClubIdsMap) {
        if (rewardClubIdsMap.isEmpty()) {
            return Map.of();
        }
        Set<Long> clubIds = rewardClubIdsMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> clubNameMap = clubRepository.findAllById(clubIds).stream()
                .collect(Collectors.toMap(ClubEntity::getId, club -> defaultString(club.getName())));
        Map<Long, List<String>> result = new LinkedHashMap<>();
        rewardClubIdsMap.forEach((rewardId, ids) -> result.put(rewardId, ids.stream()
                .map(clubNameMap::get)
                .filter(Objects::nonNull)
                .toList()));
        return result;
    }

    private List<String> loadClubNames(List<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> clubNameMap = clubRepository.findAllById(clubIds).stream()
                .collect(Collectors.toMap(ClubEntity::getId, club -> defaultString(club.getName())));
        return clubIds.stream()
                .map(clubNameMap::get)
                .filter(Objects::nonNull)
                .toList();
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

    private record RewardMutation(
            String name,
            int scoreCost,
            int stock,
            RewardItemStatus status,
            RewardVisibilityScope visibilityScope,
            List<Long> clubIds
    ) {
    }
}
