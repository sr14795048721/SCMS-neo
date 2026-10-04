package com.scms.core.reward.repository;

import com.scms.core.reward.domain.RewardOrderEntity;
import com.scms.core.reward.domain.RewardOrderStatus;
import com.scms.core.user.domain.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RewardOrderRepository extends JpaRepository<RewardOrderEntity, Long> {
    List<RewardOrderEntity> findAllByOrderByCreatedAtDescIdDesc();

    List<RewardOrderEntity> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<RewardOrderEntity> findAllByUserIdAndStatusInOrderByCreatedAtDescIdDesc(Long userId, Collection<RewardOrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from RewardOrderEntity o where o.id = :id")
    Optional<RewardOrderEntity> findByIdForUpdate(@Param("id") Long id);

    long countByRewardId(Long rewardId);

    void deleteAllByUserId(Long userId);

    @Modifying
    @Query("""
            delete from RewardOrderEntity o
            where not exists (
                select u.id
                from UserEntity u
                where u.id = o.userId
            )
               or not exists (
                select r.id
                from RewardItemEntity r
                where r.id = o.rewardId
            )
            """)
    int deleteOrphanedOrders();

    @Query("""
            select o.rewardId as rewardId, count(o.id) as orderCount
            from RewardOrderEntity o
            where o.rewardId in :rewardIds
            group by o.rewardId
            """)
    List<RewardOrderCountProjection> countByRewardIds(@Param("rewardIds") Collection<Long> rewardIds);

    @Query("""
            select coalesce(sum(o.scoreCost), 0)
            from RewardOrderEntity o
            where o.userId = :userId
              and o.status in :statuses
            """)
    Long sumScoreCostByUserIdAndStatusIn(@Param("userId") Long userId,
                                         @Param("statuses") Collection<RewardOrderStatus> statuses);
}
