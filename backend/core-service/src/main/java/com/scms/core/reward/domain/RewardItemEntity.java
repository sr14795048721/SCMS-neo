package com.scms.core.reward.domain;

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
@Table(name = "reward_items")
public class RewardItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "score_cost", nullable = false)
    private int scoreCost;

    @Column(nullable = false)
    private int stock;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "image_content_type", length = 120)
    private String imageContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RewardItemStatus status = RewardItemStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false, length = 32)
    private RewardVisibilityScope visibilityScope = RewardVisibilityScope.UNASSIGNED;

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

    public int getScoreCost() {
        return scoreCost;
    }

    public void setScoreCost(int scoreCost) {
        this.scoreCost = scoreCost;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public RewardItemStatus getStatus() {
        return status;
    }

    public void setStatus(RewardItemStatus status) {
        this.status = status;
    }

    public RewardVisibilityScope getVisibilityScope() {
        return visibilityScope;
    }

    public void setVisibilityScope(RewardVisibilityScope visibilityScope) {
        this.visibilityScope = visibilityScope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
