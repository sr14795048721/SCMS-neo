package com.scms.core.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttendanceRecordStatus status = AttendanceRecordStatus.PENDING;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(nullable = false)
    private boolean settled = false;

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

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public AttendanceRecordStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceRecordStatus status) {
        this.status = status;
    }

    public Instant getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(Instant checkInAt) {
        this.checkInAt = checkInAt;
    }

    public Instant getCheckOutAt() {
        return checkOutAt;
    }

    public void setCheckOutAt(Instant checkOutAt) {
        this.checkOutAt = checkOutAt;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
