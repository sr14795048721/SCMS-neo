package com.scms.core.telemetry.repository;

import com.scms.core.telemetry.domain.VisitorSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface VisitorSessionRepository extends JpaRepository<VisitorSessionEntity, String> {

    @Modifying
    @Query("update VisitorSessionEntity session set session.userId = null where session.userId = :userId")
    void clearUserReferenceByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO visitor_sessions (
                session_id,
                visitor_id,
                user_id,
                last_path,
                started_at,
                last_seen_at,
                user_agent
            )
            VALUES (
                :sessionId,
                :visitorId,
                :userId,
                :lastPath,
                :now,
                :now,
                :userAgent
            )
            ON CONFLICT (session_id) DO UPDATE
            SET visitor_id = EXCLUDED.visitor_id,
                user_id = COALESCE(EXCLUDED.user_id, visitor_sessions.user_id),
                last_path = EXCLUDED.last_path,
                last_seen_at = EXCLUDED.last_seen_at,
                user_agent = EXCLUDED.user_agent
            """, nativeQuery = true)
    void upsertSession(@Param("sessionId") String sessionId,
                       @Param("visitorId") String visitorId,
                       @Param("userId") Long userId,
                       @Param("lastPath") String lastPath,
                       @Param("now") Instant now,
                       @Param("userAgent") String userAgent);
}
