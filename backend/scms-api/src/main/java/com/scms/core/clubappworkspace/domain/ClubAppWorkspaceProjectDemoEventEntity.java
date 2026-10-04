package com.scms.core.clubappworkspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "club_app_workspace_project_demo_events")
public class ClubAppWorkspaceProjectDemoEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "source_session_id", nullable = false, length = 120)
    private String sourceSessionId = "";

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType = "INFO";

    @Column(nullable = false, length = 180)
    private String title = "";

    @Column(nullable = false, length = 2000)
    private String detail = "";

    @Column(nullable = false, length = 40)
    private String level = "INFO";

    @Column(name = "event_at", nullable = false)
    private Instant eventAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getSourceSessionId() {
        return sourceSessionId;
    }

    public void setSourceSessionId(String sourceSessionId) {
        this.sourceSessionId = sourceSessionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Instant getEventAt() {
        return eventAt;
    }

    public void setEventAt(Instant eventAt) {
        this.eventAt = eventAt;
    }
}
