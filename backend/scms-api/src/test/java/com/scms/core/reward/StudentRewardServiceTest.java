package com.scms.core.reward;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.reward.domain.RewardItemEntity;
import com.scms.core.reward.domain.RewardItemStatus;
import com.scms.core.reward.domain.RewardItemTargetClubEntity;
import com.scms.core.reward.domain.RewardOrderEntity;
import com.scms.core.reward.domain.RewardOrderStatus;
import com.scms.core.reward.domain.RewardVisibilityScope;
import com.scms.core.reward.dto.StudentRewardOrderCreateRequest;
import com.scms.core.reward.dto.StudentRewardOrderResponse;
import com.scms.core.reward.repository.RewardItemRepository;
import com.scms.core.reward.repository.RewardItemTargetClubRepository;
import com.scms.core.reward.repository.RewardOrderRepository;
import com.scms.core.reward.service.RewardImageStorageService;
import com.scms.core.reward.service.StudentRewardService;
import com.scms.core.score.service.ScoreBalanceService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentRewardServiceTest {

    private RewardItemRepository rewardItemRepository;
    private RewardItemTargetClubRepository rewardItemTargetClubRepository;
    private RewardOrderRepository rewardOrderRepository;
    private RewardImageStorageService rewardImageStorageService;
    private ScoreBalanceService scoreBalanceService;
    private CurrentUserProvider currentUserProvider;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private NotificationService notificationService;
    private AuditService auditService;
    private StudentRewardService studentRewardService;

    @BeforeEach
    void setUp() {
        rewardItemRepository = mock(RewardItemRepository.class);
        rewardItemTargetClubRepository = mock(RewardItemTargetClubRepository.class);
        rewardOrderRepository = mock(RewardOrderRepository.class);
        rewardImageStorageService = mock(RewardImageStorageService.class);
        scoreBalanceService = mock(ScoreBalanceService.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        notificationService = mock(NotificationService.class);
        auditService = mock(AuditService.class);

        studentRewardService = new StudentRewardService(
                rewardItemRepository,
                rewardItemTargetClubRepository,
                rewardOrderRepository,
                rewardImageStorageService,
                scoreBalanceService,
                currentUserProvider,
                clubStudentMemberRepository,
                userRepository,
                studentInfoRepository,
                notificationService,
                auditService
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(11L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.findAllByStudentUserId(anyLong())).thenReturn(List.of());
        UserEntity student = new UserEntity();
        setUserId(student, 11L);
        student.setUsername("student");
        student.setRole(UserRole.STUDENT);
        when(userRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(student));
    }

    @Test
    void createOrderShouldRejectWhenBalanceIsInsufficient() {
        RewardItemEntity reward = reward(6L, "Campus Badge", 20, 4, RewardItemStatus.ACTIVE);
        when(rewardItemRepository.findById(6L)).thenReturn(Optional.of(reward));
        when(scoreBalanceService.getBalance(11L)).thenReturn(10L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> studentRewardService.createOrder(new StudentRewardOrderCreateRequest(6L))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void createOrderShouldCreatePendingOrderAndNotifyAdmins() {
        RewardItemEntity reward = reward(6L, "Campus Badge", 20, 4, RewardItemStatus.ACTIVE);
        UserEntity admin = new UserEntity();
        setUserId(admin, 2L);
        admin.setRole(UserRole.ADMIN);
        admin.setUsername("admin");

        when(rewardItemRepository.findById(6L)).thenReturn(Optional.of(reward));
        when(scoreBalanceService.getBalance(11L)).thenReturn(80L);
        when(rewardItemRepository.decreaseStockIfAvailable(eq(6L), any(Instant.class))).thenReturn(1);
        when(userRepository.findAllSystemAdmins()).thenReturn(List.of(admin));
        StudentInfoEntity studentInfo = new StudentInfoEntity();
        studentInfo.setUserId(11L);
        studentInfo.setDisplayName("Test Student");
        when(studentInfoRepository.findByUserId(11L)).thenReturn(Optional.of(studentInfo));
        when(rewardOrderRepository.save(any(RewardOrderEntity.class))).thenAnswer(invocation -> {
            RewardOrderEntity entity = invocation.getArgument(0);
            setOrderId(entity, 15L);
            setCreatedAt(entity, Instant.now());
            return entity;
        });

        StudentRewardOrderResponse response = studentRewardService.createOrder(new StudentRewardOrderCreateRequest(6L));

        assertEquals("PENDING", response.status());
        assertEquals(15L, response.id());
        verify(notificationService).create(
                2L,
                "REWARD_ORDER",
                "student reward order created",
                "Test Student requested reward redemption: Campus Badge",
                "/sys-admin/score-reward"
        );
    }

    @Test
    void createOrderShouldRejectHiddenClubReward() {
        RewardItemEntity reward = reward(6L, "Club Only", 20, 4, RewardItemStatus.ACTIVE);
        reward.setVisibilityScope(RewardVisibilityScope.CLUB);

        RewardItemTargetClubEntity target = new RewardItemTargetClubEntity();
        target.setRewardId(6L);
        target.setClubId(9L);

        when(rewardItemRepository.findById(6L)).thenReturn(Optional.of(reward));
        when(rewardItemTargetClubRepository.findAllByRewardId(6L)).thenReturn(List.of(target));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> studentRewardService.createOrder(new StudentRewardOrderCreateRequest(6L))
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    private RewardItemEntity reward(Long id, String name, int scoreCost, int stock, RewardItemStatus status) {
        RewardItemEntity entity = new RewardItemEntity();
        setRewardId(entity, id);
        entity.setName(name);
        entity.setScoreCost(scoreCost);
        entity.setStock(stock);
        entity.setStatus(status);
        entity.setVisibilityScope(RewardVisibilityScope.GLOBAL);
        return entity;
    }

    private void setRewardId(RewardItemEntity entity, Long id) {
        try {
            var field = RewardItemEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setOrderId(RewardOrderEntity entity, Long id) {
        try {
            var field = RewardOrderEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setCreatedAt(RewardOrderEntity entity, Instant value) {
        try {
            var field = RewardOrderEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setUserId(UserEntity entity, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
