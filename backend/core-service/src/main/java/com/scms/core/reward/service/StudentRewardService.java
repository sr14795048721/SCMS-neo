package com.scms.core.reward.service;

import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.audit.service.AuditService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.reward.domain.RewardItemEntity;
import com.scms.core.reward.domain.RewardItemStatus;
import com.scms.core.reward.domain.RewardItemTargetClubEntity;
import com.scms.core.reward.domain.RewardOrderEntity;
import com.scms.core.reward.domain.RewardOrderStatus;
import com.scms.core.reward.domain.RewardVisibilityScope;
import com.scms.core.reward.dto.StudentRewardItemResponse;
import com.scms.core.reward.dto.StudentRewardOrderCreateRequest;
import com.scms.core.reward.dto.StudentRewardOrderResponse;
import com.scms.core.reward.repository.RewardItemRepository;
import com.scms.core.reward.repository.RewardItemTargetClubRepository;
import com.scms.core.reward.repository.RewardOrderRepository;
import com.scms.core.score.service.ScoreBalanceService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentRewardService {

    private final RewardItemRepository rewardItemRepository;
    private final RewardItemTargetClubRepository rewardItemTargetClubRepository;
    private final RewardOrderRepository rewardOrderRepository;
    private final RewardImageStorageService rewardImageStorageService;
    private final ScoreBalanceService scoreBalanceService;
    private final CurrentUserProvider currentUserProvider;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public StudentRewardService(RewardItemRepository rewardItemRepository,
                                RewardItemTargetClubRepository rewardItemTargetClubRepository,
                                RewardOrderRepository rewardOrderRepository,
                                RewardImageStorageService rewardImageStorageService,
                                ScoreBalanceService scoreBalanceService,
                                CurrentUserProvider currentUserProvider,
                                ClubStudentMemberRepository clubStudentMemberRepository,
                                UserRepository userRepository,
                                StudentInfoRepository studentInfoRepository,
                                NotificationService notificationService,
                                AuditService auditService) {
        this.rewardItemRepository = rewardItemRepository;
        this.rewardItemTargetClubRepository = rewardItemTargetClubRepository;
        this.rewardOrderRepository = rewardOrderRepository;
        this.rewardImageStorageService = rewardImageStorageService;
        this.scoreBalanceService = scoreBalanceService;
        this.currentUserProvider = currentUserProvider;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.userRepository = userRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StudentRewardItemResponse> listRewards() {
        AuthenticatedUser student = requireStudent();
        List<Long> clubIds = loadStudentClubIds(student.userId());
        List<RewardItemEntity> rewards = clubIds.isEmpty()
                ? rewardItemRepository.findAllByStatusAndVisibilityScopeOrderByCreatedAtDescIdDesc(
                        RewardItemStatus.ACTIVE,
                        RewardVisibilityScope.GLOBAL
                )
                : rewardItemRepository.findVisibleRewardsForStudent(RewardItemStatus.ACTIVE, clubIds);
        return rewards
                .stream()
                .filter(item -> item.getStock() > 0)
                .map(this::toRewardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentRewardOrderResponse> listMyOrders() {
        AuthenticatedUser student = requireStudent();
        List<RewardOrderEntity> orders = rewardOrderRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(student.userId());
        Map<Long, RewardItemEntity> rewardMap = loadRewardMap(orders.stream().map(RewardOrderEntity::getRewardId).collect(Collectors.toSet()));
        return orders.stream()
                .map(order -> toOrderResponse(order, rewardMap))
                .toList();
    }

    @Transactional
    public StudentRewardOrderResponse createOrder(StudentRewardOrderCreateRequest request) {
        AuthenticatedUser student = requireStudent();
        userRepository.findByIdForUpdate(student.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "student not found"));
        RewardItemEntity reward = getRequiredReward(request.rewardId());
        ensureRewardVisibleToStudent(reward, student.userId());
        if (reward.getStock() <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "reward item is out of stock");
        }

        long balanceScore = scoreBalanceService.getBalance(student.userId());
        if (balanceScore < reward.getScoreCost()) {
            throw new BusinessException(ErrorCode.CONFLICT, "insufficient score balance");
        }

        int changedRows = rewardItemRepository.decreaseStockIfAvailable(reward.getId(), Instant.now());
        if (changedRows <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "reward item is out of stock");
        }

        RewardOrderEntity entity = new RewardOrderEntity();
        entity.setRewardId(reward.getId());
        entity.setUserId(student.userId());
        entity.setScoreCost(reward.getScoreCost());
        entity.setStatus(RewardOrderStatus.PENDING);

        RewardOrderEntity saved = rewardOrderRepository.save(entity);
        auditService.create("STUDENT_REWARD_ORDER_CREATED", student.userId(), "REWARD_ORDER", String.valueOf(saved.getId()), "Student created a reward order");
        notifyAdminsForOrderCreated(resolveStudentDisplayName(student.userId()), reward.getName());
        return toOrderResponse(saved, Map.of(reward.getId(), reward));
    }

    @Transactional(readOnly = true)
    public RewardImageContent resolveImageContent(Long rewardId) {
        AuthenticatedUser student = requireStudent();
        RewardItemEntity entity = getRequiredReward(rewardId);
        ensureRewardVisibleToStudent(entity, student.userId());
        if (entity.getImagePath() == null || entity.getImagePath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "reward image not found");
        }
        return rewardImageStorageService.resolveContent(entity.getImagePath(), entity.getImageContentType());
    }

    private void notifyAdminsForOrderCreated(String studentName, String rewardName) {
        String normalizedStudentName = studentName == null || studentName.isBlank() ? "-" : studentName;
        String normalizedRewardName = rewardName == null || rewardName.isBlank() ? "-" : rewardName;
        String content = normalizedStudentName + " requested reward redemption: " + normalizedRewardName;
        for (UserEntity admin : userRepository.findAllSystemAdmins()) {
            notificationService.create(
                    admin.getId(),
                    "REWARD_ORDER",
                    "student reward order created",
                    content,
                    "/sys-admin/score-reward"
            );
        }
    }

    private String resolveStudentDisplayName(Long userId) {
        StudentInfoEntity info = studentInfoRepository.findByUserId(userId).orElse(null);
        if (info == null || info.getDisplayName() == null || info.getDisplayName().isBlank()) {
            return "";
        }
        return info.getDisplayName();
    }

    private Map<Long, RewardItemEntity> loadRewardMap(Set<Long> rewardIds) {
        if (rewardIds == null || rewardIds.isEmpty()) {
            return Map.of();
        }
        return rewardItemRepository.findAllById(rewardIds)
                .stream()
                .collect(Collectors.toMap(RewardItemEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private StudentRewardItemResponse toRewardResponse(RewardItemEntity entity) {
        return new StudentRewardItemResponse(
                entity.getId(),
                entity.getName(),
                entity.getScoreCost(),
                entity.getStock(),
                entity.getStatus().name(),
                entity.getImagePath() != null && !entity.getImagePath().isBlank(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private StudentRewardOrderResponse toOrderResponse(RewardOrderEntity entity, Map<Long, RewardItemEntity> rewardMap) {
        RewardItemEntity reward = rewardMap.get(entity.getRewardId());
        return new StudentRewardOrderResponse(
                entity.getId(),
                entity.getRewardId(),
                reward == null ? "" : reward.getName(),
                entity.getScoreCost(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getRejectedAt()
        );
    }

    private RewardItemEntity getRequiredReward(Long rewardId) {
        return rewardItemRepository.findById(rewardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "reward item not found"));
    }

    private void ensureRewardVisibleToStudent(RewardItemEntity reward, Long studentUserId) {
        if (reward.getStatus() != RewardItemStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "reward item not found");
        }
        RewardVisibilityScope scope = reward.getVisibilityScope() == null
                ? RewardVisibilityScope.UNASSIGNED
                : reward.getVisibilityScope();
        if (scope == RewardVisibilityScope.GLOBAL) {
            return;
        }
        if (scope != RewardVisibilityScope.CLUB) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "reward item not found");
        }
        Set<Long> studentClubIds = new LinkedHashSet<>(loadStudentClubIds(studentUserId));
        if (studentClubIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "reward item not found");
        }
        boolean matched = rewardItemTargetClubRepository.findAllByRewardId(reward.getId()).stream()
                .map(RewardItemTargetClubEntity::getClubId)
                .filter(Objects::nonNull)
                .anyMatch(studentClubIds::contains);
        if (!matched) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "reward item not found");
        }
    }

    private List<Long> loadStudentClubIds(Long studentUserId) {
        return clubStudentMemberRepository.findAllByStudentUserId(studentUserId).stream()
                .map(item -> item.getClubId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private AuthenticatedUser requireStudent() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (currentUser.role() != UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student role required");
        }
        return currentUser;
    }
}
