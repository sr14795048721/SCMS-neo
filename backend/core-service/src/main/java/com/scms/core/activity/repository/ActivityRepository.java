package com.scms.core.activity.repository;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.domain.ActivityStatus;
import com.scms.core.club.repository.ClubRelationCountProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ActivityEntity a where a.id = :id")
    Optional<ActivityEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select a
            from ActivityEntity a
            join com.scms.core.club.domain.ClubEntity c on c.id = a.clubId
            where (:clubId is null or a.clubId = :clubId)
              and (:status is null or a.status = :status)
              and (
                :keyword = ''
                or lower(a.title) like :keywordLike
                or lower(coalesce(a.description, '')) like :keywordLike
                or lower(coalesce(a.location, '')) like :keywordLike
                or lower(c.name) like :keywordLike
              )
            """)
    Page<ActivityEntity> searchAdmin(@Param("keyword") String keyword,
                                     @Param("keywordLike") String keywordLike,
                                     @Param("clubId") Long clubId,
                                     @Param("status") ActivityStatus status,
                                     Pageable pageable);

    @Query("""
            select a
            from ActivityEntity a
            join com.scms.core.club.domain.ClubEntity c on c.id = a.clubId
            where a.status = com.scms.core.activity.domain.ActivityStatus.PUBLISHED
              and c.status = com.scms.core.club.domain.ClubStatus.ACTIVE
              and (a.endTime is null or a.endTime >= CURRENT_TIMESTAMP)
            order by a.startTime asc, a.id desc
            """)
    List<ActivityEntity> findAllPublicVisible();

    @Query("""
            select a
            from ActivityEntity a
            join com.scms.core.club.domain.ClubEntity c on c.id = a.clubId
            where a.clubId = :clubId
              and a.status = com.scms.core.activity.domain.ActivityStatus.PUBLISHED
              and c.status = com.scms.core.club.domain.ClubStatus.ACTIVE
              and (a.endTime is null or a.endTime >= CURRENT_TIMESTAMP)
            order by a.startTime asc, a.id desc
            """)
    List<ActivityEntity> findAllStudentVisibleByClubId(@Param("clubId") Long clubId);

    List<ActivityEntity> findAllByClubIdInOrderByCreatedAtDescIdDesc(Collection<Long> clubIds);

    @Query("""
            select a.clubId as clubId, count(a.id) as relationCount
            from ActivityEntity a
            where a.clubId in :clubIds
              and a.status = :status
            group by a.clubId
            """)
    List<ClubRelationCountProjection> countByClubIdsAndStatus(@Param("clubIds") Collection<Long> clubIds,
                                                              @Param("status") ActivityStatus status);

    long countByClubId(Long clubId);

    long countByCreatedBy(Long createdBy);
}
