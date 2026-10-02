package com.scms.core.yinzhi.domain;

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
@Table(name = "yinzhi_ai_inspection_runs")
public class YinzhiAiInspectionRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telemetry_event_id", nullable = false, unique = true)
    private Long telemetryEventId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "upload_sequence", nullable = false)
    private Long uploadSequence;

    @Column(name = "trigger_reason", nullable = false, length = 80)
    private String triggerReason;

    @Column(name = "owner_presence", nullable = false, length = 32)
    private String ownerPresence;

    @Column(name = "scene_label", nullable = false, length = 120)
    private String sceneLabel;

    @Column(name = "risk_level", nullable = false, length = 40)
    private String riskLevel;

    @Column(name = "permission_decision", nullable = false, length = 80)
    private String permissionDecision;

    @Column(name = "action_summary", nullable = false, length = 500)
    private String actionSummary;

    @Column(name = "ai_provider", length = 80)
    private String aiProvider;

    @Column(name = "ai_model", length = 160)
    private String aiModel;

    @Column(name = "ai_finish_reason", length = 80)
    private String aiFinishReason;

    @Column(name = "ai_prompt_tokens", nullable = false)
    private Integer aiPromptTokens = 0;

    @Column(name = "ai_completion_tokens", nullable = false)
    private Integer aiCompletionTokens = 0;

    @Column(name = "ai_total_tokens", nullable = false)
    private Integer aiTotalTokens = 0;

    @Column(name = "ai_report", nullable = false, columnDefinition = "TEXT")
    private String aiReport;

    @Column(name = "fallback_report", nullable = false)
    private Boolean fallbackReport = false;

    @Column(name = "raw_telemetry_json", nullable = false, columnDefinition = "TEXT")
    private String rawTelemetryJson;

    @Column(name = "decision_json", nullable = false, columnDefinition = "TEXT")
    private String decisionJson;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTelemetryEventId() {
        return telemetryEventId;
    }

    public void setTelemetryEventId(Long telemetryEventId) {
        this.telemetryEventId = telemetryEventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getUploadSequence() {
        return uploadSequence;
    }

    public void setUploadSequence(Long uploadSequence) {
        this.uploadSequence = uploadSequence;
    }

    public String getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }

    public String getOwnerPresence() {
        return ownerPresence;
    }

    public void setOwnerPresence(String ownerPresence) {
        this.ownerPresence = ownerPresence;
    }

    public String getSceneLabel() {
        return sceneLabel;
    }

    public void setSceneLabel(String sceneLabel) {
        this.sceneLabel = sceneLabel;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getPermissionDecision() {
        return permissionDecision;
    }

    public void setPermissionDecision(String permissionDecision) {
        this.permissionDecision = permissionDecision;
    }

    public String getActionSummary() {
        return actionSummary;
    }

    public void setActionSummary(String actionSummary) {
        this.actionSummary = actionSummary;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getAiFinishReason() {
        return aiFinishReason;
    }

    public void setAiFinishReason(String aiFinishReason) {
        this.aiFinishReason = aiFinishReason;
    }

    public Integer getAiPromptTokens() {
        return aiPromptTokens;
    }

    public void setAiPromptTokens(Integer aiPromptTokens) {
        this.aiPromptTokens = aiPromptTokens;
    }

    public Integer getAiCompletionTokens() {
        return aiCompletionTokens;
    }

    public void setAiCompletionTokens(Integer aiCompletionTokens) {
        this.aiCompletionTokens = aiCompletionTokens;
    }

    public Integer getAiTotalTokens() {
        return aiTotalTokens;
    }

    public void setAiTotalTokens(Integer aiTotalTokens) {
        this.aiTotalTokens = aiTotalTokens;
    }

    public String getAiReport() {
        return aiReport;
    }

    public void setAiReport(String aiReport) {
        this.aiReport = aiReport;
    }

    public Boolean getFallbackReport() {
        return fallbackReport;
    }

    public void setFallbackReport(Boolean fallbackReport) {
        this.fallbackReport = fallbackReport;
    }

    public String getRawTelemetryJson() {
        return rawTelemetryJson;
    }

    public void setRawTelemetryJson(String rawTelemetryJson) {
        this.rawTelemetryJson = rawTelemetryJson;
    }

    public String getDecisionJson() {
        return decisionJson;
    }

    public void setDecisionJson(String decisionJson) {
        this.decisionJson = decisionJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
