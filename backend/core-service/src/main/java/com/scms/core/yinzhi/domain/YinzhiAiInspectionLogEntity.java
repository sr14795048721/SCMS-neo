package com.scms.core.yinzhi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "yinzhi_ai_inspection_logs")
public class YinzhiAiInspectionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "telemetry_event_id", nullable = false)
    private Long telemetryEventId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "upload_sequence", nullable = false)
    private Long uploadSequence;

    @Column(nullable = false, length = 80)
    private String stage;

    @Column(nullable = false, length = 40)
    private String level;

    @Column(nullable = false, length = 1200)
    private String message;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
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

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
