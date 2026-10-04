package com.scms.core.reward.repository;

import com.scms.core.reward.domain.RewardItemTargetClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RewardItemTargetClubRepository extends JpaRepository<RewardItemTargetClubEntity, Long> {
    List<RewardItemTargetClubEntity> findAllByRewardId(Long rewardId);

    List<RewardItemTargetClubEntity> findAllByRewardIdIn(Collection<Long> rewardIds);

    void deleteAllByRewardId(Long rewardId);

    @Modifying
    @Query("""
            delete from RewardItemTargetClubEntity t
            where not exists (
                select r.id
                from RewardItemEntity r
                where r.id = t.rewardId
            )
               or not exists (
                select c.id
                from ClubEntity c
                where c.id = t.clubId
            )
            """)
    int deleteOrphanedTargets();
}
