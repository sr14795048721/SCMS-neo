package com.scms.core.score.repository;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.score.domain.ScoreRecordEntity;
import com.scms.core.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ScoreRecordRepository extends JpaRepository<ScoreRecordEntity, Long> {
    List<ScoreRecordEntity> findAllByClubIdOrderByCreatedAtDescIdDesc(Long clubId);

    long countByRuleId(Long ruleId);

    void deleteAllByClubId(Long clubId);

    void deleteAllByUserId(Long userId);

    void deleteAllByAttendanceSessionId(Long attendanceSessionId);

    List<ScoreRecordEntity> findAllByAttendanceSessionId(Long attendanceSessionId);

    @Modifying
    @Query("""
            delete from ScoreRecordEntity s
            where not exists (
                select u.id
                from UserEntity u
                where u.id = s.userId
            )
               or not exists (
                select c.id
                from ClubEntity c
                where c.id = s.clubId
            )
            """)
    int deleteOrphanedRecords();

    @Query("""
            select coalesce(sum(s.scoreDelta), 0)
            from ScoreRecordEntity s
            where s.userId = :userId
            """)
    Long sumScoreByUserId(@Param("userId") Long userId);

    @Query("""
            select s.clubId as clubId, coalesce(sum(s.scoreDelta), 0) as totalScore
            from ScoreRecordEntity s
            where s.userId = :userId
              and s.clubId in :clubIds
            group by s.clubId
            """)
    List<ClubScoreTotalProjection> sumByUserIdGroupedByClubIds(@Param("userId") Long userId,
                                                               @Param("clubIds") Collection<Long> clubIds);

    @Query("""
            select s.clubId as clubId, coalesce(sum(s.scoreDelta), 0) as totalScore
            from ScoreRecordEntity s
            where s.clubId in :clubIds
            group by s.clubId
            """)
    List<ClubScoreTotalProjection> sumByClubIds(@Param("clubIds") Collection<Long> clubIds);

    @Query("""
            select s.userId as userId, coalesce(sum(s.scoreDelta), 0) as totalScore
            from ScoreRecordEntity s
            where s.clubId = :clubId
              and s.userId in :userIds
            group by s.userId
            """)
    List<UserScoreTotalProjection> sumByClubIdAndUserIds(@Param("clubId") Long clubId,
                                                         @Param("userIds") Collection<Long> userIds);

    @Query("""
            select s.userId as userId, coalesce(sum(s.scoreDelta), 0) as totalScore
            from ScoreRecordEntity s
            where s.clubId = :clubId
              and s.userId in :userIds
              and s.createdAt >= :startAt
            group by s.userId
            """)
    List<UserScoreTotalProjection> sumByClubIdAndUserIdsFrom(@Param("clubId") Long clubId,
                                                             @Param("userIds") Collection<Long> userIds,
                                                             @Param("startAt") Instant startAt);
}
