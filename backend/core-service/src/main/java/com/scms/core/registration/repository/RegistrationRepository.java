package com.scms.core.registration.repository;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.registration.domain.RegistrationEntity;
import com.scms.core.registration.domain.RegistrationStatus;
import com.scms.core.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<RegistrationEntity, Long> {
    Optional<RegistrationEntity> findByActivityIdAndUserId(Long activityId, Long userId);

    long countByActivityIdAndStatus(Long activityId, RegistrationStatus status);
    long countByActivityId(Long activityId);

    long countByStatus(RegistrationStatus status);

    List<RegistrationEntity> findByActivityIdAndStatusOrderByCreatedAtDesc(Long activityId, RegistrationStatus status);

    List<RegistrationEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, RegistrationStatus status);

    @Query("""
            select r.activityId as activityId, count(r.id) as registrationCount
            from RegistrationEntity r
            where r.activityId in :activityIds
              and r.status = :status
            group by r.activityId
            """)
    List<ActivityRegistrationCountProjection> countByActivityIdsAndStatus(@Param("activityIds") Collection<Long> activityIds,
                                                                          @Param("status") RegistrationStatus status);

    void deleteAllByUserId(Long userId);

    @Modifying
    @Query("""
            delete from RegistrationEntity r
            where not exists (
                select u.id
                from UserEntity u
                where u.id = r.userId
            )
               or not exists (
                select a.id
                from ActivityEntity a
                where a.id = r.activityId
            )
            """)
    int deleteOrphanedRegistrations();
}
