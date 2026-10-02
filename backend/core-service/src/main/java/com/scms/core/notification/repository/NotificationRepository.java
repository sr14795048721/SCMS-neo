package com.scms.core.notification.repository;

import com.scms.core.notification.domain.NotificationEntity;
import com.scms.core.notification.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationEntity> findTop20ByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    List<NotificationEntity> findTop3ByUserIdAndStatusOrderByCreatedAtDescIdDesc(Long userId, NotificationStatus status);

    Page<NotificationEntity> findAllByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable);

    Optional<NotificationEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    void deleteAllByUserId(Long userId);

    @Modifying
    @Query("""
            delete from NotificationEntity n
            where not exists (
                select u.id
                from UserEntity u
                where u.id = n.userId
            )
            """)
    int deleteOrphanedNotifications();
}
