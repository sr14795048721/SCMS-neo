package com.scms.core.notification;

import com.scms.core.notification.domain.NotificationEntity;
import com.scms.core.notification.domain.NotificationStatus;
import com.scms.core.notification.dto.NotificationFeedResponse;
import com.scms.core.notification.dto.NotificationSummaryResponse;
import com.scms.core.notification.repository.NotificationRepository;
import com.scms.core.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void getSummaryShouldReturnRealUnreadCountAndLatestThreeUnreadItems() {
        when(notificationRepository.countByUserIdAndStatus(5L, NotificationStatus.UNREAD)).thenReturn(6L);
        when(notificationRepository.findTop3ByUserIdAndStatusOrderByCreatedAtDescIdDesc(5L, NotificationStatus.UNREAD))
                .thenReturn(List.of(
                        notification(9L, NotificationStatus.UNREAD, "latest"),
                        notification(8L, NotificationStatus.UNREAD, "second"),
                        notification(7L, NotificationStatus.UNREAD, "third")
                ));

        NotificationSummaryResponse response = notificationService.getSummary(5L);

        assertEquals(6L, response.unreadCount());
        assertEquals(3, response.latestUnread().size());
        assertEquals("latest", response.latestUnread().get(0).title());
    }

    @Test
    void getFeedShouldNormalizeNegativePageAndClampLargeSize() {
        var pageable = PageRequest.of(0, 50);
        when(notificationRepository.findAllByUserIdAndStatus(eq(9L), eq(NotificationStatus.READ), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(notification(3L, NotificationStatus.READ, "done")), pageable, 61));

        NotificationFeedResponse response = notificationService.getFeed(9L, NotificationStatus.READ, -2, 999);

        assertEquals(0, response.page());
        assertEquals(50, response.pageSize());
        assertEquals(61, response.total());
        assertEquals(2, response.totalPages());
    }

    @Test
    void markReadShouldReturnUpdatedNotificationState() {
        NotificationEntity entity = notification(14L, NotificationStatus.UNREAD, "pending");
        when(notificationRepository.findByIdAndUserId(14L, 6L)).thenReturn(Optional.of(entity));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = notificationService.markRead(6L, 14L);

        assertEquals("READ", response.status());
        assertNull(response.targetPath());
        verify(notificationRepository).save(entity);
    }

    private NotificationEntity notification(Long id, NotificationStatus status, String title) {
        NotificationEntity entity = new NotificationEntity();
        setField(entity, "id", id);
        setField(entity, "createdAt", Instant.parse("2026-04-01T08:00:00Z"));
        entity.setUserId(1L);
        entity.setCategory("GENERAL");
        entity.setTitle(title);
        entity.setContent("content");
        entity.setStatus(status);
        return entity;
    }

    private void setField(NotificationEntity entity, String name, Object value) {
        try {
            var field = NotificationEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
