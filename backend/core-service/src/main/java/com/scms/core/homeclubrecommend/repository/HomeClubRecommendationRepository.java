package com.scms.core.homeclubrecommend.repository;

import com.scms.core.homeclubrecommend.domain.HomeClubRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HomeClubRecommendationRepository extends JpaRepository<HomeClubRecommendationEntity, Long> {
    List<HomeClubRecommendationEntity> findAllByOrderBySlotNoAsc();

    void deleteAllByClubId(Long clubId);

    void deleteAllByClubIdIn(Collection<Long> clubIds);
}
