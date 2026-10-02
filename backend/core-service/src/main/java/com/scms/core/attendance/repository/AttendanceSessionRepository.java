package com.scms.core.attendance.repository;

import com.scms.core.attendance.domain.AttendanceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSessionEntity, Long> {
    List<AttendanceSessionEntity> findAllByClubIdOrderByCreatedAtDescIdDesc(Long clubId);

    Optional<AttendanceSessionEntity> findByIdAndClubId(Long id, Long clubId);

    @Modifying
    @Query("""
            delete from AttendanceSessionEntity s
            where not exists (
                select c.id
                from ClubEntity c
                where c.id = s.clubId
            )
            """)
    int deleteOrphanedSessions();
}
