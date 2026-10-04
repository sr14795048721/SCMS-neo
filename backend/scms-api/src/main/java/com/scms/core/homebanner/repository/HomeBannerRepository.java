package com.scms.core.homebanner.repository;

import com.scms.core.homebanner.domain.BannerScope;
import com.scms.core.homebanner.domain.HomeBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HomeBannerRepository extends JpaRepository<HomeBannerEntity, Long> {

    List<HomeBannerEntity> findAllByScopeOrderBySortOrderAscIdAsc(BannerScope scope);

    Optional<HomeBannerEntity> findByIdAndScope(Long id, BannerScope scope);

    @Query("select coalesce(max(b.sortOrder), 0) from HomeBannerEntity b where b.scope = :scope")
    int findMaxSortOrderByScope(@Param("scope") BannerScope scope);
}
