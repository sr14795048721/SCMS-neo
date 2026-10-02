package com.scms.core.club.repository;

import com.scms.core.club.domain.ClubManagerBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ClubManagerBindingRepository extends JpaRepository<ClubManagerBindingEntity, Long> {
    List<ClubManagerBindingEntity> findAllByClubId(Long clubId);

    List<ClubManagerBindingEntity> findAllByClubIdIn(Collection<Long> clubIds);

    List<ClubManagerBindingEntity> findAllByManagerUserId(Long managerUserId);

    boolean existsByClubIdAndManagerUserId(Long clubId, Long managerUserId);

    void deleteAllByClubId(Long clubId);

    void deleteAllByClubIdAndManagerUserIdIn(Long clubId, Collection<Long> managerUserIds);

    void deleteAllByManagerUserId(Long managerUserId);

    @Modifying
    @Query("""
            delete from ClubManagerBindingEntity b
            where not exists (
                select u.id
                from UserEntity u
                where u.id = b.managerUserId
                  and u.role = com.scms.core.user.domain.UserRole.CLUB_MANAGER
            )
               or not exists (
                select c.id
                from ClubEntity c
                where c.id = b.clubId
            )
            """)
    int deleteOrphanedBindings();

    @Query("""
            select b.clubId as clubId, count(b.id) as relationCount
            from ClubManagerBindingEntity b
            where b.clubId in :clubIds
            group by b.clubId
            """)
    List<ClubRelationCountProjection> countByClubIds(@Param("clubIds") Collection<Long> clubIds);
}
