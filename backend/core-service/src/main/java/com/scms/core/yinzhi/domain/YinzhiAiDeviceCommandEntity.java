package com.scms.core.yinzhi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "yinzhi_ai_device_commands")
public class YinzhiAiDeviceCommandEntity {

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

    @Column(name = "command_type", nullable = false, length = 80)
    private String commandType;

    @Column(name = "risk_level", nullable = false, length = 40)
    private String riskLevel;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(nullable = false, length = 1200)
    private String detail;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt = Instant.now();

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "acked_at")
    private Instant ackedAt;

    @Column(name = "ack_message", length = 1200)
    private String ackMessage;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_telemetry_event_id")
    private Long verificationTelemetryEventId;

    @Column(name = "verification_upload_sequence")
    private Long verificationUploadSequence;

    @Column(name = "verification_message", length = 1200)
    private String verificationMessage;

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

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getAckedAt() {
        return ackedAt;
    }

    public void setAckedAt(Instant ackedAt) {
        this.ackedAt = ackedAt;
    }

    public String getAckMessage() {
        return ackMessage;
    }

    public void setAckMessage(String ackMessage) {
        this.ackMessage = ackMessage;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Long getVerificationTelemetryEventId() {
        return verificationTelemetryEventId;
    }

    public void setVerificationTelemetryEventId(Long verificationTelemetryEventId) {
        this.verificationTelemetryEventId = verificationTelemetryEventId;
    }

    public Long getVerificationUploadSequence() {
        return verificationUploadSequence;
    }

    public void setVerificationUploadSequence(Long verificationUploadSequence) {
        this.verificationUploadSequence = verificationUploadSequence;
    }

    public String getVerificationMessage() {
        return verificationMessage;
    }

    public void setVerificationMessage(String verificationMessage) {
        this.verificationMessage = verificationMessage;
    }
}
