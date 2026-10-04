package com.scms.core.student.repository;

import com.scms.core.student.domain.StudentInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentInfoRepository extends JpaRepository<StudentInfoEntity, Long> {
    Optional<StudentInfoEntity> findByUserId(Long userId);
    List<StudentInfoEntity> findAllByUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("""
            delete from StudentInfoEntity s
            where not exists (
                select u.id
                from UserEntity u
                where u.id = s.userId
                  and u.role = com.scms.core.user.domain.UserRole.STUDENT
            )
            """)
    int deleteOrphanedStudentProfiles();
}
