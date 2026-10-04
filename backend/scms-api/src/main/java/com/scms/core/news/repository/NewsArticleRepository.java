package com.scms.core.news.repository;

import com.scms.core.news.domain.NewsArticleEntity;
import com.scms.core.news.domain.NewsStatus;
import com.scms.core.user.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, Long> {

    Optional<NewsArticleEntity> findByIdAndStatus(Long id, NewsStatus status);

    Optional<NewsArticleEntity> findByIdAndAuthorIdAndStatus(Long id, Long authorId, NewsStatus status);

    Page<NewsArticleEntity> findAllByStatus(NewsStatus status, Pageable pageable);

    Page<NewsArticleEntity> findAllByAuthorIdAndStatus(Long authorId, NewsStatus status, Pageable pageable);

    Page<NewsArticleEntity> findAllByAuthorRole(UserRole authorRole, Pageable pageable);

    List<NewsArticleEntity> findTop3ByStatusAndCoverPathIsNotNullOrderByPublishedAtDescIdDesc(NewsStatus status);

    List<NewsArticleEntity> findTop3ByStatusAndCoverPathIsNullOrderByPublishedAtDescIdDesc(NewsStatus status);
}
