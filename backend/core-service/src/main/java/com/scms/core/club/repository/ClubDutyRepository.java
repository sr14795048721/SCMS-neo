package com.scms.core.club.repository;

import com.scms.core.club.domain.ClubDutyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubDutyRepository extends JpaRepository<ClubDutyEntity, Long> {
    List<ClubDutyEntity> findAllByClubIdOrderByCreatedAtAscIdAsc(Long clubId);

    List<ClubDutyEntity> findAllByClubIdIn(Collection<Long> clubIds);

    List<ClubDutyEntity> findAllByIdIn(Collection<Long> dutyIds);

    Optional<ClubDutyEntity> findByIdAndClubId(Long id, Long clubId);

    boolean existsByClubIdAndNameIgnoreCase(Long clubId, String name);

    boolean existsByClubIdAndNameIgnoreCaseAndIdNot(Long clubId, String name, Long id);

    @Modifying
    @Query("""
            delete from ClubDutyEntity d
            where not exists (
                select c.id
                from ClubEntity c
                where c.id = d.clubId
            )
            """)
    int deleteOrphanedDuties();
}
