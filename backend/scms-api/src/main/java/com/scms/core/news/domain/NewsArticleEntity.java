package com.scms.core.news.domain;

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
@Table(name = "news_articles")
public class NewsArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title = "";

    @Column(name = "markdown_content", nullable = false, columnDefinition = "TEXT")
    private String markdownContent = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NewsStatus status = NewsStatus.DRAFT;

    @Column(name = "cover_path", length = 255)
    private String coverPath;

    @Column(name = "cover_content_type", length = 128)
    private String coverContentType;

    @Column(name = "cover_size_bytes")
    private Long coverSizeBytes;

    @Column(name = "cover_updated_at")
    private Instant coverUpdatedAt;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 32)
    private com.scms.core.user.domain.UserRole authorRole = com.scms.core.user.domain.UserRole.ADMIN;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

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

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public NewsStatus getStatus() {
        return status;
    }

    public void setStatus(NewsStatus status) {
        this.status = status;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getCoverContentType() {
        return coverContentType;
    }

    public void setCoverContentType(String coverContentType) {
        this.coverContentType = coverContentType;
    }

    public Long getCoverSizeBytes() {
        return coverSizeBytes;
    }

    public void setCoverSizeBytes(Long coverSizeBytes) {
        this.coverSizeBytes = coverSizeBytes;
    }

    public Instant getCoverUpdatedAt() {
        return coverUpdatedAt;
    }

    public void setCoverUpdatedAt(Instant coverUpdatedAt) {
        this.coverUpdatedAt = coverUpdatedAt;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public com.scms.core.user.domain.UserRole getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(com.scms.core.user.domain.UserRole authorRole) {
        this.authorRole = authorRole;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
