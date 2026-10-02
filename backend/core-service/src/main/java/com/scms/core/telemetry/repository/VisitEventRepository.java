package com.scms.core.telemetry.repository;

import com.scms.core.telemetry.domain.VisitEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitEventRepository extends JpaRepository<VisitEventEntity, Long> {

    @Modifying
    @Query("update VisitEventEntity event set event.userId = null where event.userId = :userId")
    void clearUserReferenceByUserId(@Param("userId") Long userId);
}
