package com.scms.core.attendance.repository;

import com.scms.core.attendance.domain.AttendanceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {
    List<AttendanceRecordEntity> findAllBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    List<AttendanceRecordEntity> findAllBySessionIdIn(Collection<Long> sessionIds);

    Optional<AttendanceRecordEntity> findBySessionIdAndStudentUserId(Long sessionId, Long studentUserId);

    void deleteAllByStudentUserId(Long studentUserId);

    @Modifying
    void deleteAllBySessionId(Long sessionId);

    @Modifying
    @Query("""
            delete from AttendanceRecordEntity r
            where not exists (
                select s.id
                from AttendanceSessionEntity s
                where s.id = r.sessionId
            )
               or not exists (
                select u.id
                from UserEntity u
                where u.id = r.studentUserId
                  and u.role = com.scms.core.user.domain.UserRole.STUDENT
            )
            """)
    int deleteOrphanedRecords();
}
