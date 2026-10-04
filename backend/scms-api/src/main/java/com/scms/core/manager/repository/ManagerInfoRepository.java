package com.scms.core.manager.repository;

import com.scms.core.manager.domain.ManagerInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ManagerInfoRepository extends JpaRepository<ManagerInfoEntity, Long> {
    Optional<ManagerInfoEntity> findByUserId(Long userId);
    List<ManagerInfoEntity> findAllByUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("""
            delete from ManagerInfoEntity m
            where not exists (
                select u.id
                from UserEntity u
                where u.id = m.userId
                  and u.role = com.scms.core.user.domain.UserRole.CLUB_MANAGER
            )
            """)
    int deleteOrphanedManagerProfiles();
}
