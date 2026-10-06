package com.scms.core.competition.domain;

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
@Table(name = "competitions")
public class CompetitionEntity {

    public enum CompetitionStatus {
        UPCOMING,
        ONGOING,
        ENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 120)
    private String category;

    @Column(name = "participant_scope", length = 200)
    private String participantScope;

    @Column(name = "apply_deadline")
    private Instant applyDeadline;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(length = 500)
    private String url;

    @Column(name = "source_id")
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CompetitionStatus status = CompetitionStatus.UPCOMING;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "last_deadline_notified_at")
    private Instant lastDeadlineNotifiedAt;

    @Column(name = "last_start_notified_at")
    private Instant lastStartNotifiedAt;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getParticipantScope() {
        return participantScope;
    }

    public void setParticipantScope(String participantScope) {
        this.participantScope = participantScope;
    }

    public Instant getApplyDeadline() {
        return applyDeadline;
    }

    public void setApplyDeadline(Instant applyDeadline) {
        this.applyDeadline = applyDeadline;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public CompetitionStatus getStatus() {
        return status;
    }

    public void setStatus(CompetitionStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getLastDeadlineNotifiedAt() {
        return lastDeadlineNotifiedAt;
    }

    public void setLastDeadlineNotifiedAt(Instant lastDeadlineNotifiedAt) {
        this.lastDeadlineNotifiedAt = lastDeadlineNotifiedAt;
    }

    public Instant getLastStartNotifiedAt() {
        return lastStartNotifiedAt;
    }

    public void setLastStartNotifiedAt(Instant lastStartNotifiedAt) {
        this.lastStartNotifiedAt = lastStartNotifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
