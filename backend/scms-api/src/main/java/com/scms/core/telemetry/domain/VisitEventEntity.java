package com.scms.core.telemetry.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "visit_events")
public class VisitEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false, length = 64)
    private String visitorId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;

    @PrePersist
    void onCreate() {
        if (this.visitedAt == null) {
            this.visitedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }
}
