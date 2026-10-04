package com.scms.core.reward.repository;

import com.scms.core.reward.domain.RewardItemEntity;
import com.scms.core.reward.domain.RewardItemStatus;
import com.scms.core.reward.domain.RewardVisibilityScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RewardItemRepository extends JpaRepository<RewardItemEntity, Long> {
    @Query("""
            select r
            from RewardItemEntity r
            where (:status is null or r.status = :status)
              and (
                :keyword = ''
                or lower(r.name) like :keywordLike
              )
            order by r.createdAt desc, r.id desc
            """)
    List<RewardItemEntity> searchAdmin(@Param("keyword") String keyword,
                                       @Param("keywordLike") String keywordLike,
                                       @Param("status") RewardItemStatus status);

    List<RewardItemEntity> findAllByOrderByCreatedAtDescIdDesc();

    List<RewardItemEntity> findAllByStatusOrderByCreatedAtDescIdDesc(RewardItemStatus status);

    List<RewardItemEntity> findAllByStatusAndVisibilityScopeOrderByCreatedAtDescIdDesc(RewardItemStatus status,
                                                                                        RewardVisibilityScope visibilityScope);

    @Query("""
            select r
            from RewardItemEntity r
            where r.status = :status
              and (
                r.visibilityScope = com.scms.core.reward.domain.RewardVisibilityScope.GLOBAL
                or (
                    r.visibilityScope = com.scms.core.reward.domain.RewardVisibilityScope.CLUB
                    and exists (
                        select 1
                        from RewardItemTargetClubEntity target
                        where target.rewardId = r.id
                          and target.clubId in :clubIds
                    )
                )
              )
            order by r.createdAt desc, r.id desc
            """)
    List<RewardItemEntity> findVisibleRewardsForStudent(@Param("status") RewardItemStatus status,
                                                        @Param("clubIds") List<Long> clubIds);

    @Modifying
    @Query("""
            update RewardItemEntity r
            set r.stock = r.stock - 1,
                r.updatedAt = :updatedAt
            where r.id = :rewardId
              and r.stock > 0
            """)
    int decreaseStockIfAvailable(@Param("rewardId") Long rewardId, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("""
            update RewardItemEntity r
            set r.stock = r.stock + 1,
                r.updatedAt = :updatedAt
            where r.id = :rewardId
            """)
    int increaseStock(@Param("rewardId") Long rewardId, @Param("updatedAt") Instant updatedAt);
}
