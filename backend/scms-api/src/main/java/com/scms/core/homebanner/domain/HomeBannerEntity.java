package com.scms.core.homebanner.domain;

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
@Table(name = "home_banners")
public class HomeBannerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title = "";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private HomeBannerMediaType mediaType = HomeBannerMediaType.IMAGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 16)
    private BannerScope scope = BannerScope.HOME;

    @Column(name = "media_path", length = 255)
    private String mediaPath;

    @Column(name = "media_content_type", length = 128)
    private String mediaContentType;

    @Column(name = "media_size_bytes")
    private Long mediaSizeBytes;

    @Column(name = "media_optimized_version")
    private Integer mediaOptimizedVersion;

    @Column(name = "poster_path", length = 255)
    private String posterPath;

    @Column(name = "poster_content_type", length = 128)
    private String posterContentType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

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

    public HomeBannerMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(HomeBannerMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public BannerScope getScope() {
        return scope;
    }

    public void setScope(BannerScope scope) {
        this.scope = scope;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public String getMediaContentType() {
        return mediaContentType;
    }

    public void setMediaContentType(String mediaContentType) {
        this.mediaContentType = mediaContentType;
    }

    public Long getMediaSizeBytes() {
        return mediaSizeBytes;
    }

    public void setMediaSizeBytes(Long mediaSizeBytes) {
        this.mediaSizeBytes = mediaSizeBytes;
    }

    public Integer getMediaOptimizedVersion() {
        return mediaOptimizedVersion;
    }

    public void setMediaOptimizedVersion(Integer mediaOptimizedVersion) {
        this.mediaOptimizedVersion = mediaOptimizedVersion;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getPosterContentType() {
        return posterContentType;
    }

    public void setPosterContentType(String posterContentType) {
        this.posterContentType = posterContentType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
