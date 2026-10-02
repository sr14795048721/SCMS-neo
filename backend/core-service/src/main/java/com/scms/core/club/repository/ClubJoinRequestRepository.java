package com.scms.core.club.repository;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubJoinRequestEntity;
import com.scms.core.club.domain.ClubJoinRequestStatus;
import com.scms.core.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubJoinRequestRepository extends JpaRepository<ClubJoinRequestEntity, Long> {
    List<ClubJoinRequestEntity> findAllByClubIdOrderByCreatedAtDescIdDesc(Long clubId);

    List<ClubJoinRequestEntity> findAllByStudentUserIdAndStatusOrderByCreatedAtAscIdAsc(Long studentUserId,
                                                                                        ClubJoinRequestStatus status);

    Optional<ClubJoinRequestEntity> findByIdAndClubId(Long id, Long clubId);

    boolean existsByClubIdAndStudentUserIdAndStatus(Long clubId, Long studentUserId, ClubJoinRequestStatus status);

    boolean existsByStudentUserIdAndStatus(Long studentUserId, ClubJoinRequestStatus status);

    void deleteAllByStudentUserId(Long studentUserId);

    @Modifying
    @Query("""
            delete from ClubJoinRequestEntity r
            where not exists (
                select u.id
                from UserEntity u
                where u.id = r.studentUserId
                  and u.role = com.scms.core.user.domain.UserRole.STUDENT
            )
               or not exists (
                select c.id
                from ClubEntity c
                where c.id = r.clubId
            )
            """)
    int deleteOrphanedJoinRequests();

    long countByClubIdAndStatus(Long clubId, ClubJoinRequestStatus status);

    @Query("""
            select r.clubId as clubId, count(r.id) as relationCount
            from ClubJoinRequestEntity r
            where r.clubId in :clubIds
              and r.status = :status
            group by r.clubId
            """)
    List<ClubRelationCountProjection> countByClubIdsAndStatus(@Param("clubIds") Collection<Long> clubIds,
                                                              @Param("status") ClubJoinRequestStatus status);
}
