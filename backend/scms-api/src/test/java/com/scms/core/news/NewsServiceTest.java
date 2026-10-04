package com.scms.core.news;

import com.scms.core.common.exception.BusinessException;
import com.scms.core.news.domain.NewsArticleEntity;
import com.scms.core.news.domain.NewsAssetEntity;
import com.scms.core.news.domain.NewsStatus;
import com.scms.core.news.dto.UpdateNewsRequest;
import com.scms.core.news.repository.NewsArticleRepository;
import com.scms.core.news.repository.NewsAssetRepository;
import com.scms.core.news.service.NewsService;
import com.scms.core.news.service.NewsStorageService;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsServiceTest {

    private NewsArticleRepository newsArticleRepository;
    private NewsAssetRepository newsAssetRepository;
    private UserRepository userRepository;
    private CurrentUserProvider currentUserProvider;
    private NewsStorageService newsStorageService;
    private NotificationService notificationService;
    private NewsService newsService;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        newsArticleRepository = mock(NewsArticleRepository.class);
        newsAssetRepository = mock(NewsAssetRepository.class);
        userRepository = mock(UserRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        newsStorageService = mock(NewsStorageService.class);
        notificationService = mock(NotificationService.class);
        newsService = new NewsService(
                newsArticleRepository,
                newsAssetRepository,
                userRepository,
                currentUserProvider,
                newsStorageService,
                notificationService
        );

        adminUser = new UserEntity();
        setEntityId(adminUser, 1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@scms.local");
        adminUser.setPasswordHash("encoded");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
    }

    @Test
    void createDraftShouldInitializeAdminDraftArticle() {
        when(newsArticleRepository.save(any(NewsArticleEntity.class))).thenAnswer(invocation -> {
            NewsArticleEntity entity = invocation.getArgument(0);
            setArticleId(entity, 9L);
            return entity;
        });

        var response = newsService.createDraft();

        assertEquals(9L, response.id());
        assertEquals("DRAFT", response.status());
        assertEquals("", response.title());
        assertEquals("admin", response.authorName());
    }

    @Test
    void publishShouldRejectEmptyDraftContent() {
        NewsArticleEntity article = buildArticle(7L, "", "", NewsStatus.DRAFT);
        when(newsArticleRepository.findById(7L)).thenReturn(Optional.of(article));

        assertThrows(
                BusinessException.class,
                () -> newsService.update(7L, new UpdateNewsRequest(null, null, "PUBLISHED"))
        );
    }

    @Test
    void updateShouldCleanupUnreferencedAssetsWhenMarkdownChanges() {
        NewsArticleEntity article = buildArticle(7L, "Title", "old", NewsStatus.DRAFT);
        NewsAssetEntity firstAsset = buildAsset(11L, 7L, "content-images/1.jpg");
        NewsAssetEntity secondAsset = buildAsset(12L, 7L, "content-images/2.jpg");

        when(newsArticleRepository.findById(7L)).thenReturn(Optional.of(article));
        when(newsAssetRepository.findAllByNewsIdOrderByIdAsc(7L)).thenReturn(List.of(firstAsset, secondAsset));
        when(newsArticleRepository.save(any(NewsArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = newsService.update(
                7L,
                new UpdateNewsRequest(
                        "Updated",
                        "![](/api/v1/public/news/7/assets/11)",
                        "DRAFT"
                )
        );

        assertEquals("Updated", response.title());
        assertEquals("DRAFT", response.status());
    }

    @Test
    void updateShouldRetainReferencedManagerAssetUrls() {
        NewsArticleEntity article = buildArticle(8L, "Teacher Draft", "old", NewsStatus.DRAFT);
        NewsAssetEntity firstAsset = buildAsset(21L, 8L, "content-images/21.jpg");
        NewsAssetEntity secondAsset = buildAsset(22L, 8L, "content-images/22.jpg");

        when(newsArticleRepository.findById(8L)).thenReturn(Optional.of(article));
        when(newsAssetRepository.findAllByNewsIdOrderByIdAsc(8L)).thenReturn(List.of(firstAsset, secondAsset));
        when(newsArticleRepository.save(any(NewsArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = newsService.update(
                8L,
                new UpdateNewsRequest(
                        "Teacher Draft",
                        "![](http://localhost:3000/api/manager/news/8/assets/21/content)",
                        "DRAFT"
                )
        );

        assertEquals("Teacher Draft", response.title());
        verify(newsAssetRepository).deleteAll(argThat(assets -> {
            List<? extends NewsAssetEntity> list = StreamSupport.stream(assets.spliterator(), false).toList();
            return list.size() == 1 && list.get(0).getId().equals(22L);
        }));
        verify(newsStorageService).deleteIfExists("content-images/22.jpg");
        verify(newsStorageService, never()).deleteIfExists("content-images/21.jpg");
    }

    @Test
    void deleteShouldRemoveAssetsBeforeDeletingArticleAndCleanupFiles() {
        NewsArticleEntity article = buildArticle(7L, "Title", "content", NewsStatus.DRAFT);
        article.setCoverPath("covers/cover.jpg");
        NewsAssetEntity firstAsset = buildAsset(11L, 7L, "content-images/1.jpg");
        NewsAssetEntity secondAsset = buildAsset(12L, 7L, "content-images/2.jpg");

        when(newsArticleRepository.findById(7L)).thenReturn(Optional.of(article));
        when(newsAssetRepository.findAllByNewsIdOrderByIdAsc(7L)).thenReturn(List.of(firstAsset, secondAsset));

        newsService.delete(7L);

        var inOrder = inOrder(newsAssetRepository, newsArticleRepository);
        inOrder.verify(newsAssetRepository).deleteAll(List.of(firstAsset, secondAsset));
        inOrder.verify(newsArticleRepository).delete(article);
        verify(newsStorageService).deleteIfExists("covers/cover.jpg");
        verify(newsStorageService).deleteIfExists("content-images/1.jpg");
        verify(newsStorageService).deleteIfExists("content-images/2.jpg");
    }

    private NewsArticleEntity buildArticle(Long id, String title, String markdown, NewsStatus status) {
        NewsArticleEntity article = new NewsArticleEntity();
        setArticleId(article, id);
        article.setTitle(title);
        article.setMarkdownContent(markdown);
        article.setStatus(status);
        article.setAuthorId(1L);
        article.setAuthorRole(UserRole.ADMIN);
        article.setCreatedBy(1L);
        article.setUpdatedBy(1L);
        article.setViewCount(0L);
        article.setPublishedAt(Instant.now());
        return article;
    }

    private NewsAssetEntity buildAsset(Long id, Long newsId, String filePath) {
        NewsAssetEntity asset = new NewsAssetEntity();
        setAssetId(asset, id);
        asset.setNewsId(newsId);
        asset.setFilePath(filePath);
        asset.setContentType("image/jpeg");
        asset.setSizeBytes(2048L);
        asset.setCreatedBy(1L);
        return asset;
    }

    @Test
    void publishTeacherDraftShouldNotifyAuthor() {
        NewsArticleEntity article = buildArticle(21L, "Teacher Draft", "hello", NewsStatus.DRAFT);
        article.setAuthorId(9L);
        article.setAuthorRole(UserRole.CLUB_MANAGER);
        article.setPublishedAt(null);
        when(newsArticleRepository.findById(21L)).thenReturn(Optional.of(article));
        when(newsArticleRepository.save(any(NewsArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = newsService.update(21L, new UpdateNewsRequest("Teacher Draft", "hello", "PUBLISHED"));

        assertEquals("PUBLISHED", response.status());
        verify(notificationService).create(
                9L,
                "NEWS",
                "teacher news published",
                "Administrator published your news draft to the homepage news feed.",
                "/news/21.html"
        );
    }

    @Test
    void publishAdminDraftShouldNotNotifyTeacher() {
        NewsArticleEntity article = buildArticle(22L, "Admin Draft", "hello", NewsStatus.DRAFT);
        when(newsArticleRepository.findById(22L)).thenReturn(Optional.of(article));
        when(newsArticleRepository.save(any(NewsArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        newsService.update(22L, new UpdateNewsRequest("Admin Draft", "hello", "PUBLISHED"));

        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }

    private void setEntityId(UserEntity user, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setArticleId(NewsArticleEntity article, Long id) {
        try {
            var field = NewsArticleEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(article, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setAssetId(NewsAssetEntity asset, Long id) {
        try {
            var field = NewsAssetEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(asset, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
