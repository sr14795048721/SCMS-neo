package com.scms.core.iot.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "iot_device_telemetry_events")
public class IotDeviceTelemetryEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, length = 64)
    private String schemaName;

    @Column(nullable = false, length = 64)
    private String project;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "gateway_type", nullable = false, length = 64)
    private String gatewayType;

    @Column(length = 128)
    private String firmware;

    @Column(length = 32)
    private String mac;

    @Column(name = "event_name", nullable = false, length = 64)
    private String eventName;

    @Column(name = "upload_sequence", nullable = false)
    private Long uploadSequence;

    @Column(name = "sent_at_ms", nullable = false)
    private Long sentAtMs;

    @Column(name = "gateway_uptime_ms")
    private Long gatewayUptimeMs;

    @Column(name = "pending_reason", length = 64)
    private String pendingReason;

    @Column(name = "wifi_connected", nullable = false)
    private Boolean wifiConnected;

    @Column(length = 64)
    private String ip;

    @Column
    private Integer rssi;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @PrePersist
    void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(String gatewayType) {
        this.gatewayType = gatewayType;
    }

    public String getFirmware() {
        return firmware;
    }

    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Long getUploadSequence() {
        return uploadSequence;
    }

    public void setUploadSequence(Long uploadSequence) {
        this.uploadSequence = uploadSequence;
    }

    public Long getSentAtMs() {
        return sentAtMs;
    }

    public void setSentAtMs(Long sentAtMs) {
        this.sentAtMs = sentAtMs;
    }

    public Long getGatewayUptimeMs() {
        return gatewayUptimeMs;
    }

    public void setGatewayUptimeMs(Long gatewayUptimeMs) {
        this.gatewayUptimeMs = gatewayUptimeMs;
    }

    public String getPendingReason() {
        return pendingReason;
    }

    public void setPendingReason(String pendingReason) {
        this.pendingReason = pendingReason;
    }

    public Boolean getWifiConnected() {
        return wifiConnected;
    }

    public void setWifiConnected(Boolean wifiConnected) {
        this.wifiConnected = wifiConnected;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
