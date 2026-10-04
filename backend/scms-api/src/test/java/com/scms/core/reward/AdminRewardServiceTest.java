package com.scms.core.reward;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.reward.domain.RewardItemEntity;
import com.scms.core.reward.domain.RewardOrderEntity;
import com.scms.core.reward.domain.RewardOrderStatus;
import com.scms.core.reward.dto.AdminRewardMutationRequest;
import com.scms.core.reward.dto.AdminRewardOrderResponse;
import com.scms.core.reward.repository.RewardItemRepository;
import com.scms.core.reward.repository.RewardItemTargetClubRepository;
import com.scms.core.reward.repository.RewardOrderRepository;
import com.scms.core.reward.service.AdminRewardService;
import com.scms.core.reward.service.RewardImageStorageService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminRewardServiceTest {

    private RewardItemRepository rewardItemRepository;
    private RewardItemTargetClubRepository rewardItemTargetClubRepository;
    private RewardOrderRepository rewardOrderRepository;
    private RewardImageStorageService rewardImageStorageService;
    private ClubRepository clubRepository;
    private UserRepository userRepository;
    private StudentInfoRepository studentInfoRepository;
    private CurrentUserProvider currentUserProvider;
    private NotificationService notificationService;
    private AuditService auditService;
    private AdminRewardService adminRewardService;

    @BeforeEach
    void setUp() {
        rewardItemRepository = mock(RewardItemRepository.class);
        rewardItemTargetClubRepository = mock(RewardItemTargetClubRepository.class);
        rewardOrderRepository = mock(RewardOrderRepository.class);
        rewardImageStorageService = mock(RewardImageStorageService.class);
        clubRepository = mock(ClubRepository.class);
        userRepository = mock(UserRepository.class);
        studentInfoRepository = mock(StudentInfoRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        notificationService = mock(NotificationService.class);
        auditService = mock(AuditService.class);

        adminRewardService = new AdminRewardService(
                rewardItemRepository,
                rewardItemTargetClubRepository,
                rewardOrderRepository,
                rewardImageStorageService,
                clubRepository,
                userRepository,
                studentInfoRepository,
                currentUserProvider,
                notificationService,
                auditService
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
    }

    @Test
    void deleteRewardShouldRejectReferencedReward() {
        RewardItemEntity reward = new RewardItemEntity();
        setRewardId(reward, 5L);

        when(rewardItemRepository.findById(5L)).thenReturn(Optional.of(reward));
        when(rewardOrderRepository.countByRewardId(5L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminRewardService.deleteReward(5L));

        assertEquals(ErrorCode.DEPENDENCY_EXISTS, exception.getErrorCode());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteOrderShouldRejectPendingOrder() {
        RewardOrderEntity order = new RewardOrderEntity();
        setOrderId(order, 8L);
        order.setStatus(RewardOrderStatus.PENDING);

        when(rewardOrderRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminRewardService.deleteOrder(8L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verifyNoInteractions(auditService);
    }

    @Test
    void rejectOrderShouldRejectCompletedOrder() {
        RewardOrderEntity order = new RewardOrderEntity();
        setOrderId(order, 9L);
        order.setStatus(RewardOrderStatus.COMPLETED);

        when(rewardOrderRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminRewardService.rejectOrder(9L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verifyNoInteractions(auditService);
    }

    @Test
    void listAdminOrdersShouldSupportPendingOrdersWithoutReviewerFields() {
        RewardItemEntity reward = new RewardItemEntity();
        setRewardId(reward, 5L);
        reward.setName("Badge");

        RewardOrderEntity order = new RewardOrderEntity();
        setOrderId(order, 11L);
        order.setRewardId(5L);
        order.setUserId(4L);
        order.setScoreCost(5);
        order.setStatus(RewardOrderStatus.PENDING);

        UserEntity student = new UserEntity();
        setUserId(student, 4L);
        student.setUsername("waleden");
        student.setRole(UserRole.STUDENT);

        StudentInfoEntity studentInfo = new StudentInfoEntity();
        studentInfo.setUserId(4L);
        studentInfo.setDisplayName("Walter White");
        studentInfo.setStudentNo("2026001");

        when(rewardOrderRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(order));
        when(rewardItemRepository.findAllById(anyCollection())).thenReturn(List.of(reward));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(student));
        when(studentInfoRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(studentInfo));

        List<AdminRewardOrderResponse> result = adminRewardService.listAdminOrders();

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).status());
        assertEquals("Badge", result.get(0).rewardName());
        assertEquals("Walter White", result.get(0).displayName());
    }

    @Test
    void createRewardShouldRejectUnassignedVisibilityScope() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminRewardService.createReward(new AdminRewardMutationRequest(
                        "Campus Badge",
                        20,
                        10,
                        "UNASSIGNED",
                        List.of(),
                        "ACTIVE"
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void createRewardShouldRejectClubVisibilityWithoutTargets() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminRewardService.createReward(new AdminRewardMutationRequest(
                        "Campus Badge",
                        20,
                        10,
                        "CLUB",
                        List.of(),
                        "ACTIVE"
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verifyNoInteractions(rewardItemRepository);
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
