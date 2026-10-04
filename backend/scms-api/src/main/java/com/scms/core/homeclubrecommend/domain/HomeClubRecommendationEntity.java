package com.scms.core.homeclubrecommend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "home_club_recommendations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_home_club_recommendations_club", columnNames = "club_id"),
                @UniqueConstraint(name = "uk_home_club_recommendations_slot", columnNames = "slot_no")
        }
)
public class HomeClubRecommendationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "slot_no", nullable = false)
    private Short slotNo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public Short getSlotNo() {
        return slotNo;
    }

    public void setSlotNo(Short slotNo) {
        this.slotNo = slotNo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
