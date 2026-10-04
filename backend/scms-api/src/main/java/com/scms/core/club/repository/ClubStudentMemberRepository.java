package com.scms.core.club.repository;

import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ClubStudentMemberRepository extends JpaRepository<ClubStudentMemberEntity, Long> {
    List<ClubStudentMemberEntity> findAllByClubId(Long clubId);

    List<ClubStudentMemberEntity> findAllByClubIdIn(Collection<Long> clubIds);

    List<ClubStudentMemberEntity> findAllByStudentUserId(Long studentUserId);

    List<ClubStudentMemberEntity> findAllByStudentUserIdOrderByCreatedAtAscIdAsc(Long studentUserId);

    List<ClubStudentMemberEntity> findAllByDutyId(Long dutyId);

    List<ClubStudentMemberEntity> findAllByClubIdAndDutyId(Long clubId, Long dutyId);

    java.util.Optional<ClubStudentMemberEntity> findByClubIdAndStudentUserId(Long clubId, Long studentUserId);

    boolean existsByClubIdAndStudentUserId(Long clubId, Long studentUserId);

    boolean existsByStudentUserId(Long studentUserId);

    void deleteAllByClubId(Long clubId);

    void deleteAllByStudentUserId(Long studentUserId);

    @Modifying
    @Query("""
            delete from ClubStudentMemberEntity m
            where not exists (
                select u.id
                from UserEntity u
                where u.id = m.studentUserId
                  and u.role = com.scms.core.user.domain.UserRole.STUDENT
            )
               or not exists (
                select c.id
                from ClubEntity c
                where c.id = m.clubId
            )
               or (
                m.dutyId is not null
                and not exists (
                    select d.id
                    from ClubDutyEntity d
                    where d.id = m.dutyId
                      and d.clubId = m.clubId
                )
            )
            """)
    int deleteOrphanedMemberships();

    @Query("""
            select b.clubId as clubId, count(b.id) as relationCount
            from ClubStudentMemberEntity b
            where b.clubId in :clubIds
            group by b.clubId
            """)
    List<ClubRelationCountProjection> countByClubIds(@Param("clubIds") Collection<Long> clubIds);
}
