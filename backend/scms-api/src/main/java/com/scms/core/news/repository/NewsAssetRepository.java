package com.scms.core.news.repository;

import com.scms.core.news.domain.NewsAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsAssetRepository extends JpaRepository<NewsAssetEntity, Long> {

    List<NewsAssetEntity> findAllByNewsIdOrderByIdAsc(Long newsId);

    Optional<NewsAssetEntity> findByIdAndNewsId(Long id, Long newsId);
}
