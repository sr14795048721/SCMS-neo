package com.scms.core.notification.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.notification.domain.NotificationEntity;
import com.scms.core.notification.domain.NotificationStatus;
import com.scms.core.notification.dto.NotificationFeedResponse;
import com.scms.core.notification.dto.NotificationResponse;
import com.scms.core.notification.dto.NotificationSummaryResponse;
import com.scms.core.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final int SUMMARY_LIMIT = 3;
    private static final int DEFAULT_FEED_SIZE = 20;
    private static final int MAX_FEED_SIZE = 50;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationEntity create(Long userId, String category, String title, String content) {
        return create(userId, category, title, content, null);
    }

    @Transactional
    public NotificationEntity create(Long userId, String category, String title, String content, String targetPath) {
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setCategory(category);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setTargetPath(normalizeTargetPath(targetPath));
        entity.setStatus(NotificationStatus.UNREAD);
        return notificationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listLatest(Long userId) {
        return listLatest(userId, null);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listLatest(Long userId, NotificationStatus status) {
        List<NotificationEntity> notifications = status == null
                ? notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                : notificationRepository.findTop20ByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getSummary(Long userId) {
        long unreadCount = countUnread(userId);
        List<NotificationResponse> latestUnread = notificationRepository
                .findTop3ByUserIdAndStatusOrderByCreatedAtDescIdDesc(userId, NotificationStatus.UNREAD)
                .stream()
                .limit(SUMMARY_LIMIT)
                .map(this::toResponse)
                .toList();
        return new NotificationSummaryResponse(unreadCount, latestUnread);
    }

    @Transactional(readOnly = true)
    public NotificationFeedResponse getFeed(Long userId, NotificationStatus status, Integer page, Integer size) {
        NotificationStatus requiredStatus = status == null ? NotificationStatus.UNREAD : status;
        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<NotificationEntity> result = notificationRepository.findAllByUserIdAndStatus(userId, requiredStatus, pageable);
        List<NotificationResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new NotificationFeedResponse(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        NotificationEntity entity = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "notification not found"));
        entity.setStatus(NotificationStatus.READ);
        NotificationEntity saved = notificationRepository.save(entity);
        return toResponse(saved);
    }

    private String normalizeTargetPath(String targetPath) {
        String normalized = String.valueOf(targetPath == null ? "" : targetPath).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_FEED_SIZE;
        }
        return Math.min(size, MAX_FEED_SIZE);
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getContent(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getTargetPath()
        );
    }
}
