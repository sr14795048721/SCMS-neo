package com.scms.core.clubappworkspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "club_app_workspace_project_demo_profiles")
public class ClubAppWorkspaceProjectDemoProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "overview_title", nullable = false, length = 180)
    private String overviewTitle = "";

    @Column(name = "overview_body", nullable = false, length = 4000)
    private String overviewBody = "";

    @Column(name = "bridge_mode", nullable = false, length = 60)
    private String bridgeMode = "APP_HUB";

    @Column(name = "mock_snapshot_json", nullable = false, length = 20000)
    private String mockSnapshotJson = "{}";

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getOverviewTitle() {
        return overviewTitle;
    }

    public void setOverviewTitle(String overviewTitle) {
        this.overviewTitle = overviewTitle;
    }

    public String getOverviewBody() {
        return overviewBody;
    }

    public void setOverviewBody(String overviewBody) {
        this.overviewBody = overviewBody;
    }

    public String getBridgeMode() {
        return bridgeMode;
    }

    public void setBridgeMode(String bridgeMode) {
        this.bridgeMode = bridgeMode;
    }

    public String getMockSnapshotJson() {
        return mockSnapshotJson;
    }

    public void setMockSnapshotJson(String mockSnapshotJson) {
        this.mockSnapshotJson = mockSnapshotJson;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
