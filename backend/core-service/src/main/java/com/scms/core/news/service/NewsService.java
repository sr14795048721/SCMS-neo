package com.scms.core.news.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.news.domain.NewsArticleEntity;
import com.scms.core.news.domain.NewsAssetEntity;
import com.scms.core.news.domain.NewsAssetType;
import com.scms.core.news.domain.NewsStatus;
import com.scms.core.news.dto.AdminNewsDetailResponse;
import com.scms.core.news.dto.AdminNewsListItemResponse;
import com.scms.core.news.dto.AdminNewsPageResponse;
import com.scms.core.news.dto.PublicNewsCardResponse;
import com.scms.core.news.dto.PublicNewsDetailResponse;
import com.scms.core.news.dto.PublicNewsHomeResponse;
import com.scms.core.news.dto.PublicNewsListItemResponse;
import com.scms.core.news.dto.PublicNewsPageResponse;
import com.scms.core.news.dto.UpdateNewsRequest;
import com.scms.core.news.dto.UploadedNewsAssetResponse;
import com.scms.core.news.repository.NewsArticleRepository;
import com.scms.core.news.repository.NewsAssetRepository;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class NewsService {

    private static final Pattern REFERENCED_ASSET_PATTERN = Pattern.compile(
            "(?:https?://[^\\s)]+)?/api(?:/v1)?/(?:public/news|managers/me/news|manager/news|admin/news)/(\\d+)/assets/(\\d+)(?:/content)?(?:\\?[^\\s)]*)?"
    );
    private static final int HOME_LIMIT = 3;

    private final NewsArticleRepository newsArticleRepository;
    private final NewsAssetRepository newsAssetRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NewsStorageService newsStorageService;
    private final NotificationService notificationService;

    public NewsService(NewsArticleRepository newsArticleRepository,
                       NewsAssetRepository newsAssetRepository,
                       UserRepository userRepository,
                       CurrentUserProvider currentUserProvider,
                       NewsStorageService newsStorageService,
                       NotificationService notificationService) {
        this.newsArticleRepository = newsArticleRepository;
        this.newsAssetRepository = newsAssetRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.newsStorageService = newsStorageService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminNewsPageResponse listAdmin(int page, int pageSize, String source) {
        UserRole authorRole = parseAuthorRole(source);
        Pageable pageable = PageRequest.of(
                normalizePage(page) - 1,
                normalizePageSize(pageSize),
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );
        Page<NewsArticleEntity> result = authorRole == null
                ? newsArticleRepository.findAll(pageable)
                : newsArticleRepository.findAllByAuthorRole(authorRole, pageable);
        Map<Long, String> authorNames = resolveAuthorNames(result.getContent());
        List<AdminNewsListItemResponse> items = result.getContent()
                .stream()
                .map(article -> toAdminListItem(article, authorNames))
                .toList();
        return new AdminNewsPageResponse(items, result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public AdminNewsDetailResponse createDraft() {
        AuthenticatedUser user = getRequiredAdminUser();
        return createDraftForAuthor(user, user.role());
    }

    @Transactional(readOnly = true)
    public AdminNewsPageResponse listManager(int page, int pageSize) {
        AuthenticatedUser user = getRequiredManagerUser();
        Pageable pageable = PageRequest.of(
                normalizePage(page) - 1,
                normalizePageSize(pageSize),
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );
        Page<NewsArticleEntity> result = newsArticleRepository.findAllByAuthorIdAndStatus(user.userId(), NewsStatus.DRAFT, pageable);
        Map<Long, String> authorNames = resolveAuthorNames(result.getContent());
        List<AdminNewsListItemResponse> items = result.getContent()
                .stream()
                .map(article -> toAdminListItem(article, authorNames))
                .toList();
        return new AdminNewsPageResponse(items, result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public AdminNewsDetailResponse createManagerDraft() {
        AuthenticatedUser user = getRequiredManagerUser();
        return createDraftForAuthor(user, UserRole.CLUB_MANAGER);
    }

    @Transactional(readOnly = true)
    public AdminNewsDetailResponse getManagerDetail(Long newsId) {
        AuthenticatedUser user = getRequiredManagerUser();
        NewsArticleEntity article = getRequiredManagerDraft(newsId, user.userId());
        return toAdminDetail(article, resolveAuthorName(article.getAuthorId()));
    }

    @Transactional
    public AdminNewsDetailResponse updateManagerDraft(Long newsId, UpdateNewsRequest request) {
        if (request.status() != null) {
            try {
                if (NewsStatus.fromValue(request.status()) == NewsStatus.PUBLISHED) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "manager cannot publish news");
                }
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid news status");
            }
        }

        AuthenticatedUser user = getRequiredManagerUser();
        NewsArticleEntity article = getRequiredManagerDraft(newsId, user.userId());
        return updateDraft(article, request, user.userId(), false);
    }

    @Transactional
    public AdminNewsDetailResponse uploadManagerCover(Long newsId, MultipartFile file) {
        AuthenticatedUser user = getRequiredManagerUser();
        NewsArticleEntity article = getRequiredManagerDraft(newsId, user.userId());
        return uploadCover(article, file, user.userId());
    }

    @Transactional
    public AdminNewsDetailResponse deleteManagerCover(Long newsId) {
        AuthenticatedUser user = getRequiredManagerUser();
        NewsArticleEntity article = getRequiredManagerDraft(newsId, user.userId());
        return deleteCover(article, user.userId());
    }

    @Transactional
    public UploadedNewsAssetResponse uploadManagerBodyImage(Long newsId, MultipartFile file) {
        AuthenticatedUser user = getRequiredManagerUser();
        NewsArticleEntity article = getRequiredManagerDraft(newsId, user.userId());
        return uploadBodyImage(article, file, user.userId());
    }

    @Transactional
    public void deleteManagerDraft(Long newsId) {
        AuthenticatedUser user = getRequiredManagerUser();
        NewsArticleEntity article = getRequiredManagerDraft(newsId, user.userId());
        deleteArticle(article);
    }

    @Transactional(readOnly = true)
    public NewsMediaContent resolveManagerCoverContent(Long newsId) {
        AuthenticatedUser user = getRequiredManagerUser();
        NewsArticleEntity article = getRequiredManagerDraft(newsId, user.userId());
        if (article.getCoverPath() == null || article.getCoverPath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "news cover not found");
        }
        return newsStorageService.resolveContent(article.getCoverPath(), article.getCoverContentType());
    }

    @Transactional(readOnly = true)
    public NewsMediaContent resolveManagerAssetContent(Long newsId, Long assetId) {
        AuthenticatedUser user = getRequiredManagerUser();
        getRequiredManagerDraft(newsId, user.userId());
        NewsAssetEntity asset = newsAssetRepository.findByIdAndNewsId(assetId, newsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "news asset not found"));
        return newsStorageService.resolveContent(asset.getFilePath(), asset.getContentType());
    }

    private AdminNewsDetailResponse createDraftForAuthor(AuthenticatedUser user, UserRole authorRole) {
        NewsArticleEntity entity = new NewsArticleEntity();
        entity.setTitle("");
        entity.setMarkdownContent("");
        entity.setStatus(NewsStatus.DRAFT);
        entity.setAuthorId(user.userId());
        entity.setAuthorRole(authorRole);
        entity.setCreatedBy(user.userId());
        entity.setUpdatedBy(user.userId());
        entity.setViewCount(0L);
        NewsArticleEntity saved = newsArticleRepository.save(entity);
        return toAdminDetail(saved, resolveAuthorName(saved.getAuthorId()));
    }

    @Transactional(readOnly = true)
    public AdminNewsDetailResponse getAdminDetail(Long newsId) {
        NewsArticleEntity article = getRequiredArticle(newsId);
        return toAdminDetail(article, resolveAuthorName(article.getAuthorId()));
    }

    @Transactional
    public AdminNewsDetailResponse update(Long newsId, UpdateNewsRequest request) {
        NewsArticleEntity article = getRequiredArticle(newsId);
        AuthenticatedUser user = getRequiredAdminUser();
        return updateDraft(article, request, user.userId(), true);
    }

    private AdminNewsDetailResponse updateDraft(NewsArticleEntity article,
                                                UpdateNewsRequest request,
                                                Long operatorUserId,
                                                boolean allowPublish) {
        boolean wasPublished = article.getStatus() == NewsStatus.PUBLISHED;
        if (request.title() != null) {
            article.setTitle(normalizeTitle(request.title()));
        }
        if (request.markdownContent() != null) {
            article.setMarkdownContent(normalizeMarkdown(request.markdownContent()));
        }
        if (request.status() != null) {
            NewsStatus nextStatus;
            try {
                nextStatus = NewsStatus.fromValue(request.status());
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid news status");
            }
            if (!allowPublish && nextStatus == NewsStatus.PUBLISHED) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "manager cannot publish news");
            }
            if (nextStatus == NewsStatus.PUBLISHED) {
                validatePublishable(article);
                if (article.getPublishedAt() == null) {
                    article.setPublishedAt(Instant.now());
                }
            }
            article.setStatus(nextStatus);
        }

        article.setUpdatedBy(operatorUserId);
        NewsArticleEntity saved = newsArticleRepository.save(article);
        if (request.markdownContent() != null) {
            cleanupUnreferencedAssets(saved);
        }
        if (allowPublish
                && !wasPublished
                && saved.getStatus() == NewsStatus.PUBLISHED
                && saved.getAuthorRole() == UserRole.CLUB_MANAGER) {
            notificationService.create(
                    saved.getAuthorId(),
                    "NEWS",
                    "teacher news published",
                    "Administrator published your news draft to the homepage news feed.",
                    "/news/" + saved.getId() + ".html"
            );
        }
        return toAdminDetail(saved, resolveAuthorName(saved.getAuthorId()));
    }

    @Transactional
    public AdminNewsDetailResponse uploadCover(Long newsId, MultipartFile file) {
        NewsArticleEntity article = getRequiredArticle(newsId);
        AuthenticatedUser user = getRequiredAdminUser();
        return uploadCover(article, file, user.userId());
    }

    private AdminNewsDetailResponse uploadCover(NewsArticleEntity article, MultipartFile file, Long operatorUserId) {
        StoredNewsFile storedFile = newsStorageService.storeCover(file);
        String previousCoverPath = article.getCoverPath();

        try {
            article.setCoverPath(storedFile.relativePath());
            article.setCoverContentType(storedFile.contentType());
            article.setCoverSizeBytes(storedFile.sizeBytes());
            article.setCoverUpdatedAt(Instant.now());
            article.setUpdatedBy(operatorUserId);
            NewsArticleEntity saved = newsArticleRepository.save(article);
            newsStorageService.deleteIfExists(previousCoverPath);
            return toAdminDetail(saved, resolveAuthorName(saved.getAuthorId()));
        } catch (RuntimeException exception) {
            newsStorageService.deleteIfExists(storedFile.relativePath());
            throw exception;
        }
    }

    @Transactional
    public AdminNewsDetailResponse deleteCover(Long newsId) {
        NewsArticleEntity article = getRequiredArticle(newsId);
        AuthenticatedUser user = getRequiredAdminUser();
        return deleteCover(article, user.userId());
    }

    private AdminNewsDetailResponse deleteCover(NewsArticleEntity article, Long operatorUserId) {
        String previousCoverPath = article.getCoverPath();

        article.setCoverPath(null);
        article.setCoverContentType(null);
        article.setCoverSizeBytes(null);
        article.setCoverUpdatedAt(null);
        article.setUpdatedBy(operatorUserId);
        NewsArticleEntity saved = newsArticleRepository.save(article);
        newsStorageService.deleteIfExists(previousCoverPath);
        return toAdminDetail(saved, resolveAuthorName(saved.getAuthorId()));
    }

    @Transactional
    public UploadedNewsAssetResponse uploadBodyImage(Long newsId, MultipartFile file) {
        NewsArticleEntity article = getRequiredArticle(newsId);
        AuthenticatedUser user = getRequiredAdminUser();
        return uploadBodyImage(article, file, user.userId());
    }

    private UploadedNewsAssetResponse uploadBodyImage(NewsArticleEntity article, MultipartFile file, Long operatorUserId) {
        StoredNewsFile storedFile = newsStorageService.storeBodyImage(file);

        try {
            NewsAssetEntity asset = new NewsAssetEntity();
            asset.setNewsId(article.getId());
            asset.setAssetType(NewsAssetType.BODY_IMAGE);
            asset.setFilePath(storedFile.relativePath());
            asset.setContentType(storedFile.contentType());
            asset.setSizeBytes(storedFile.sizeBytes());
            asset.setCreatedBy(operatorUserId);
            NewsAssetEntity saved = newsAssetRepository.save(asset);
            String url = buildAssetPath(article.getId(), saved.getId());
            return new UploadedNewsAssetResponse(saved.getId(), url, "![](" + url + ")");
        } catch (RuntimeException exception) {
            newsStorageService.deleteIfExists(storedFile.relativePath());
            throw exception;
        }
    }

    @Transactional
    public void delete(Long newsId) {
        NewsArticleEntity article = getRequiredArticle(newsId);
        deleteArticle(article);
    }

    private void deleteArticle(NewsArticleEntity article) {
        List<NewsAssetEntity> assets = newsAssetRepository.findAllByNewsIdOrderByIdAsc(article.getId());
        if (!assets.isEmpty()) {
            newsAssetRepository.deleteAll(assets);
        }
        newsArticleRepository.delete(article);

        newsStorageService.deleteIfExists(article.getCoverPath());
        for (NewsAssetEntity asset : assets) {
            newsStorageService.deleteIfExists(asset.getFilePath());
        }
    }

    @Transactional(readOnly = true)
    public PublicNewsHomeResponse home() {
        List<NewsArticleEntity> carouselArticles = newsArticleRepository
                .findTop3ByStatusAndCoverPathIsNotNullOrderByPublishedAtDescIdDesc(NewsStatus.PUBLISHED);
        List<NewsArticleEntity> textArticles = newsArticleRepository
                .findTop3ByStatusAndCoverPathIsNullOrderByPublishedAtDescIdDesc(NewsStatus.PUBLISHED);
        Map<Long, String> authorNames = resolveAuthorNames(Stream.concat(carouselArticles.stream(), textArticles.stream()).toList());

        List<PublicNewsCardResponse> carousel = carouselArticles.stream()
                .limit(HOME_LIMIT)
                .map(article -> toPublicCard(article, authorNames))
                .toList();
        List<PublicNewsCardResponse> textList = textArticles.stream()
                .limit(HOME_LIMIT)
                .map(article -> toPublicCard(article, authorNames))
                .toList();
        return new PublicNewsHomeResponse(carousel, textList);
    }

    @Transactional(readOnly = true)
    public PublicNewsPageResponse listPublic(int page, int pageSize) {
        Pageable pageable = PageRequest.of(
                normalizePage(page) - 1,
                normalizePageSize(pageSize),
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
        );
        Page<NewsArticleEntity> result = newsArticleRepository.findAllByStatus(NewsStatus.PUBLISHED, pageable);
        Map<Long, String> authorNames = resolveAuthorNames(result.getContent());
        List<PublicNewsListItemResponse> items = result.getContent()
                .stream()
                .map(article -> toPublicListItem(article, authorNames))
                .toList();
        return new PublicNewsPageResponse(items, result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public PublicNewsDetailResponse getPublicDetail(Long newsId) {
        NewsArticleEntity article = getRequiredPublishedArticle(newsId);
        article.setViewCount(article.getViewCount() == null ? 1L : article.getViewCount() + 1L);
        NewsArticleEntity saved = newsArticleRepository.save(article);
        return toPublicDetail(saved, resolveAuthorName(saved.getAuthorId()));
    }

    @Transactional(readOnly = true)
    public NewsMediaContent resolveCoverContent(Long newsId) {
        NewsArticleEntity article = getRequiredPublishedArticle(newsId);
        if (article.getCoverPath() == null || article.getCoverPath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "news cover not found");
        }
        return newsStorageService.resolveContent(article.getCoverPath(), article.getCoverContentType());
    }

    @Transactional(readOnly = true)
    public NewsMediaContent resolveAdminCoverContent(Long newsId) {
        NewsArticleEntity article = getRequiredArticle(newsId);
        if (article.getCoverPath() == null || article.getCoverPath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "news cover not found");
        }
        return newsStorageService.resolveContent(article.getCoverPath(), article.getCoverContentType());
    }

    @Transactional(readOnly = true)
    public NewsMediaContent resolveAssetContent(Long newsId, Long assetId) {
        getRequiredPublishedArticle(newsId);
        NewsAssetEntity asset = newsAssetRepository.findByIdAndNewsId(assetId, newsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "news asset not found"));
        return newsStorageService.resolveContent(asset.getFilePath(), asset.getContentType());
    }

    @Transactional(readOnly = true)
    public NewsMediaContent resolveAdminAssetContent(Long newsId, Long assetId) {
        getRequiredArticle(newsId);
        NewsAssetEntity asset = newsAssetRepository.findByIdAndNewsId(assetId, newsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "news asset not found"));
        return newsStorageService.resolveContent(asset.getFilePath(), asset.getContentType());
    }

    private void validatePublishable(NewsArticleEntity article) {
        if (article.getTitle() == null || article.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "news title is required");
        }
        if (article.getMarkdownContent() == null || article.getMarkdownContent().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "news markdown content is required");
        }
    }

    private void cleanupUnreferencedAssets(NewsArticleEntity article) {
        List<NewsAssetEntity> assets = newsAssetRepository.findAllByNewsIdOrderByIdAsc(article.getId());
        if (assets.isEmpty()) {
            return;
        }

        Set<Long> referencedIds = extractReferencedAssetIds(article.getId(), article.getMarkdownContent());
        List<NewsAssetEntity> staleAssets = assets.stream()
                .filter(asset -> !referencedIds.contains(asset.getId()))
                .toList();
        if (staleAssets.isEmpty()) {
            return;
        }

        newsAssetRepository.deleteAll(staleAssets);
        for (NewsAssetEntity staleAsset : staleAssets) {
            newsStorageService.deleteIfExists(staleAsset.getFilePath());
        }
    }

    private Set<Long> extractReferencedAssetIds(Long newsId, String markdownContent) {
        Set<Long> assetIds = new HashSet<>();
        Matcher matcher = REFERENCED_ASSET_PATTERN.matcher(String.valueOf(markdownContent == null ? "" : markdownContent));
        while (matcher.find()) {
            long matchedNewsId = Long.parseLong(matcher.group(1));
            if (matchedNewsId != newsId) {
                continue;
            }
            assetIds.add(Long.parseLong(matcher.group(2)));
        }
        return assetIds;
    }

    private Map<Long, String> resolveAuthorNames(List<NewsArticleEntity> articles) {
        Set<Long> authorIds = articles.stream()
                .map(NewsArticleEntity::getAuthorId)
                .collect(HashSet::new, Set::add, Set::addAll);
        Map<Long, String> names = new HashMap<>();
        if (authorIds.isEmpty()) {
            return names;
        }

        for (UserEntity user : userRepository.findAllById(authorIds)) {
            names.put(user.getId(), user.getUsername());
        }
        return names;
    }

    private String resolveAuthorName(Long authorId) {
        return userRepository.findById(authorId)
                .map(UserEntity::getUsername)
                .orElse("admin");
    }

    private NewsArticleEntity getRequiredArticle(Long newsId) {
        return newsArticleRepository.findById(newsId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "news not found"));
    }

    private NewsArticleEntity getRequiredManagerDraft(Long newsId, Long managerUserId) {
        return newsArticleRepository.findByIdAndAuthorIdAndStatus(newsId, managerUserId, NewsStatus.DRAFT)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "news not found"));
    }

    private NewsArticleEntity getRequiredPublishedArticle(Long newsId) {
        return newsArticleRepository.findByIdAndStatus(newsId, NewsStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "news not found"));
    }

    private AuthenticatedUser getRequiredAdminUser() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (!user.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return user;
    }

    private AuthenticatedUser getRequiredManagerUser() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager role required");
        }
        return user;
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }

    private String normalizeTitle(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        if (normalized.length() > 180) {
            normalized = normalized.substring(0, 180).trim();
        }
        return normalized;
    }

    private String normalizeMarkdown(String value) {
        return String.valueOf(value == null ? "" : value).trim();
    }

    private AdminNewsListItemResponse toAdminListItem(NewsArticleEntity article, Map<Long, String> authorNames) {
        return new AdminNewsListItemResponse(
                article.getId(),
                article.getTitle(),
                article.getStatus().name(),
                article.getCoverPath() != null && !article.getCoverPath().isBlank(),
                article.getCoverPath() != null ? buildCoverPath(article.getId()) : null,
                authorNames.getOrDefault(article.getAuthorId(), "admin"),
                article.getAuthorRole() == null ? UserRole.ADMIN.name() : article.getAuthorRole().name(),
                article.getViewCount() == null ? 0L : article.getViewCount(),
                article.getPublishedAt(),
                article.getUpdatedAt()
        );
    }

    private AdminNewsDetailResponse toAdminDetail(NewsArticleEntity article, String authorName) {
        return new AdminNewsDetailResponse(
                article.getId(),
                article.getTitle(),
                article.getStatus().name(),
                article.getMarkdownContent(),
                article.getCoverPath() != null ? buildCoverPath(article.getId()) : null,
                authorName,
                article.getAuthorRole() == null ? UserRole.ADMIN.name() : article.getAuthorRole().name(),
                article.getViewCount() == null ? 0L : article.getViewCount(),
                article.getPublishedAt(),
                article.getCoverUpdatedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private PublicNewsCardResponse toPublicCard(NewsArticleEntity article, Map<Long, String> authorNames) {
        return new PublicNewsCardResponse(
                article.getId(),
                article.getTitle(),
                article.getCoverPath() != null ? buildCoverPath(article.getId()) : null,
                authorNames.getOrDefault(article.getAuthorId(), "admin"),
                article.getPublishedAt(),
                article.getViewCount() == null ? 0L : article.getViewCount()
        );
    }

    private PublicNewsListItemResponse toPublicListItem(NewsArticleEntity article, Map<Long, String> authorNames) {
        return new PublicNewsListItemResponse(
                article.getId(),
                article.getTitle(),
                article.getCoverPath() != null ? buildCoverPath(article.getId()) : null,
                authorNames.getOrDefault(article.getAuthorId(), "admin"),
                article.getPublishedAt(),
                article.getViewCount() == null ? 0L : article.getViewCount()
        );
    }

    private PublicNewsDetailResponse toPublicDetail(NewsArticleEntity article, String authorName) {
        return new PublicNewsDetailResponse(
                article.getId(),
                article.getTitle(),
                article.getCoverPath() != null ? buildCoverPath(article.getId()) : null,
                authorName,
                article.getPublishedAt(),
                article.getViewCount() == null ? 0L : article.getViewCount(),
                article.getMarkdownContent()
        );
    }

    private String buildCoverPath(Long newsId) {
        return "/api/v1/public/news/" + newsId + "/cover";
    }

    private String buildAssetPath(Long newsId, Long assetId) {
        return "/api/v1/public/news/" + newsId + "/assets/" + assetId;
    }

    private UserRole parseAuthorRole(String source) {
        String normalized = String.valueOf(source == null ? "" : source).trim().toUpperCase();
        if (normalized.isEmpty() || normalized.equals("ALL")) {
            return null;
        }
        try {
            UserRole role = UserRole.valueOf(normalized);
            return role == UserRole.STUDENT ? null : role;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
