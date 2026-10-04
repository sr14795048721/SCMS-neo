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
@Table(name = "club_app_workspace_project_demo_steps")
public class ClubAppWorkspaceProjectDemoStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demo_profile_id", nullable = false)
    private Long demoProfileId;

    @Column(nullable = false, length = 180)
    private String title = "";

    @Column(nullable = false, length = 1000)
    private String description = "";

    @Column(name = "trigger_type", nullable = false, length = 40)
    private String triggerType = "MANUAL";

    @Column(name = "target_subsystem", nullable = false, length = 60)
    private String targetSubsystem = "APP";

    @Column(name = "action_key", length = 120)
    private String actionKey;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

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

    public Long getDemoProfileId() {
        return demoProfileId;
    }

    public void setDemoProfileId(Long demoProfileId) {
        this.demoProfileId = demoProfileId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTargetSubsystem() {
        return targetSubsystem;
    }

    public void setTargetSubsystem(String targetSubsystem) {
        this.targetSubsystem = targetSubsystem;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
