package com.scms.core.club.repository;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ClubRepository extends JpaRepository<ClubEntity, Long> {
    Optional<ClubEntity> findByName(String name);
    Optional<ClubEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            select c
            from ClubEntity c
            where (:status is null or c.status = :status)
              and (
                :keyword = ''
                or lower(c.name) like :keywordLike
                or lower(coalesce(c.type, '')) like :keywordLike
                or lower(coalesce(c.description, '')) like :keywordLike
              )
            """)
    Page<ClubEntity> searchAdmin(@Param("keyword") String keyword,
                                 @Param("keywordLike") String keywordLike,
                                 @Param("status") ClubStatus status,
                                 Pageable pageable);

    List<ClubEntity> findAllByStatusOrderByCreatedAtDescIdDesc(ClubStatus status);

    long countByCreatedBy(Long createdBy);
}
