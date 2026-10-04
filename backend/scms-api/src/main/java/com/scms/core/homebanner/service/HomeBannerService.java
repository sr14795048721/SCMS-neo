package com.scms.core.homebanner.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.homebanner.domain.BannerScope;
import com.scms.core.homebanner.domain.HomeBannerEntity;
import com.scms.core.homebanner.domain.HomeBannerMediaType;
import com.scms.core.homebanner.dto.AdminBannerRecompressFailureResponse;
import com.scms.core.homebanner.dto.AdminBannerRecompressResponse;
import com.scms.core.homebanner.dto.AdminHomeBannerResponse;
import com.scms.core.homebanner.dto.PublicHomeBannerItemResponse;
import com.scms.core.homebanner.dto.UpdateHomeBannerRequest;
import com.scms.core.homebanner.repository.HomeBannerRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HomeBannerService {

    private final HomeBannerRepository homeBannerRepository;
    private final CurrentUserProvider currentUserProvider;
    private final BannerMediaStorageService bannerMediaStorageService;

    public HomeBannerService(HomeBannerRepository homeBannerRepository,
                             CurrentUserProvider currentUserProvider,
                             BannerMediaStorageService bannerMediaStorageService) {
        this.homeBannerRepository = homeBannerRepository;
        this.currentUserProvider = currentUserProvider;
        this.bannerMediaStorageService = bannerMediaStorageService;
    }

    @Transactional(readOnly = true)
    public List<AdminHomeBannerResponse> listAdmin(BannerScope scope) {
        return homeBannerRepository.findAllByScopeOrderBySortOrderAscIdAsc(scope)
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicHomeBannerItemResponse> listPublic(BannerScope scope) {
        return homeBannerRepository.findAllByScopeOrderBySortOrderAscIdAsc(scope)
                .stream()
                .filter(this::hasMedia)
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional
    public AdminHomeBannerResponse create(BannerScope scope) {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        HomeBannerEntity entity = new HomeBannerEntity();
        entity.setScope(scope);
        entity.setTitle("");
        entity.setDescription("");
        entity.setMediaType(HomeBannerMediaType.IMAGE);
        entity.setSortOrder(homeBannerRepository.findMaxSortOrderByScope(scope) + 1);
        entity.setCreatedBy(user.userId());
        entity.setUpdatedBy(user.userId());
        return toAdminResponse(homeBannerRepository.save(entity));
    }

    @Transactional
    public AdminHomeBannerResponse update(BannerScope scope, Long bannerId, UpdateHomeBannerRequest request) {
        HomeBannerEntity entity = getRequiredBanner(scope, bannerId);
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        String oldMediaPath = null;
        String oldPosterPath = null;

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle().trim());
        }
        if (request.getDesc() != null) {
            entity.setDescription(request.getDesc().trim());
        }
        if (request.getType() != null) {
            HomeBannerMediaType nextType;
            try {
                nextType = HomeBannerMediaType.fromValue(request.getType());
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid banner type");
            }
            if (entity.getMediaType() != nextType) {
                oldMediaPath = entity.getMediaPath();
                oldPosterPath = entity.getPosterPath();
                clearMedia(entity);
                entity.setMediaType(nextType);
            }
        }

        entity.setUpdatedBy(user.userId());
        HomeBannerEntity saved = homeBannerRepository.save(entity);
        bannerMediaStorageService.deleteIfExists(oldMediaPath);
        bannerMediaStorageService.deleteIfExists(oldPosterPath);
        return toAdminResponse(saved);
    }

    @Transactional
    public AdminHomeBannerResponse uploadFile(BannerScope scope, Long bannerId, MultipartFile file) {
        HomeBannerEntity entity = getRequiredBanner(scope, bannerId);
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        StoredBannerMedia stored = bannerMediaStorageService.store(entity.getMediaType(), file);

        String previousMediaPath = entity.getMediaPath();
        String previousPosterPath = entity.getPosterPath();
        String newMediaPath = stored.mediaPath();
        String newPosterPath = stored.posterPath();
        try {
            entity.setMediaPath(stored.mediaPath());
            entity.setMediaContentType(stored.mediaContentType());
            entity.setMediaSizeBytes(stored.mediaSizeBytes());
            entity.setMediaOptimizedVersion(stored.mediaOptimizedVersion());
            entity.setPosterPath(stored.posterPath());
            entity.setPosterContentType(stored.posterContentType());
            entity.setUpdatedBy(user.userId());
            HomeBannerEntity saved = homeBannerRepository.save(entity);
            bannerMediaStorageService.deleteIfExists(previousMediaPath);
            bannerMediaStorageService.deleteIfExists(previousPosterPath);
            return toAdminResponse(saved);
        } catch (RuntimeException exception) {
            bannerMediaStorageService.deleteIfExists(newMediaPath);
            bannerMediaStorageService.deleteIfExists(newPosterPath);
            throw exception;
        }
    }

    @Transactional
    public List<AdminHomeBannerResponse> reorder(BannerScope scope, List<Long> ids) {
        List<HomeBannerEntity> banners = homeBannerRepository.findAllByScopeOrderBySortOrderAscIdAsc(scope);
        validateOrderPayload(banners, ids);

        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        int sortOrder = 1;
        for (Long id : ids) {
            HomeBannerEntity entity = banners.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "home banner not found"));
            entity.setSortOrder(sortOrder++);
            entity.setUpdatedBy(user.userId());
        }

        return homeBannerRepository.saveAll(banners)
                .stream()
                .sorted((left, right) -> {
                    int bySort = Integer.compare(left.getSortOrder(), right.getSortOrder());
                    if (bySort != 0) {
                        return bySort;
                    }
                    return Long.compare(left.getId(), right.getId());
                })
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminBannerRecompressResponse recompress(BannerScope scope) {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        int currentOptimizationVersion = bannerMediaStorageService.getBannerOptimizationVersion();
        List<HomeBannerEntity> banners = homeBannerRepository.findAllByScopeOrderBySortOrderAscIdAsc(scope);
        List<HomeBannerEntity> updatedBanners = new ArrayList<>();
        List<AdminBannerRecompressFailureResponse> failures = new ArrayList<>();
        int processedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (HomeBannerEntity entity : banners) {
            if (entity.getMediaType() != HomeBannerMediaType.IMAGE || !hasMedia(entity)) {
                skippedCount++;
                continue;
            }
            if (isAlreadyOptimized(entity, currentOptimizationVersion)) {
                skippedCount++;
                continue;
            }

            try {
                StoredBannerMedia stored = bannerMediaStorageService.recompressImage(
                        entity.getMediaPath(),
                        entity.getMediaContentType()
                );
                entity.setMediaContentType(stored.mediaContentType());
                entity.setMediaSizeBytes(stored.mediaSizeBytes());
                entity.setMediaOptimizedVersion(stored.mediaOptimizedVersion());
                entity.setUpdatedBy(user.userId());
                updatedBanners.add(entity);
                processedCount++;
            } catch (RuntimeException exception) {
                failedCount++;
                failures.add(new AdminBannerRecompressFailureResponse(
                        entity.getId(),
                        resolveFailureMessage(exception)
                ));
            }
        }

        if (!updatedBanners.isEmpty()) {
            homeBannerRepository.saveAll(updatedBanners);
        }

        return new AdminBannerRecompressResponse(
                processedCount,
                skippedCount,
                failedCount,
                failures
        );
    }

    @Transactional
    public void delete(BannerScope scope, Long bannerId) {
        HomeBannerEntity entity = getRequiredBanner(scope, bannerId);
        String mediaPath = entity.getMediaPath();
        String posterPath = entity.getPosterPath();
        int deletedSortOrder = entity.getSortOrder();
        homeBannerRepository.delete(entity);

        List<HomeBannerEntity> remaining = homeBannerRepository.findAllByScopeOrderBySortOrderAscIdAsc(scope);
        for (HomeBannerEntity item : remaining) {
            if (item.getSortOrder() > deletedSortOrder) {
                item.setSortOrder(item.getSortOrder() - 1);
            }
        }
        homeBannerRepository.saveAll(remaining);
        bannerMediaStorageService.deleteIfExists(mediaPath);
        bannerMediaStorageService.deleteIfExists(posterPath);
    }

    @Transactional(readOnly = true)
    public BannerMediaContent resolveMainContent(BannerScope scope, Long bannerId) {
        HomeBannerEntity entity = getRequiredBanner(scope, bannerId);
        if (!hasMedia(entity)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "banner media not found");
        }
        return bannerMediaStorageService.resolveContent(entity.getMediaPath(), entity.getMediaContentType());
    }

    @Transactional(readOnly = true)
    public BannerMediaContent resolvePosterContent(BannerScope scope, Long bannerId) {
        HomeBannerEntity entity = getRequiredBanner(scope, bannerId);
        if (entity.getPosterPath() == null || entity.getPosterPath().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "banner poster not found");
        }
        return bannerMediaStorageService.resolveContent(entity.getPosterPath(), entity.getPosterContentType());
    }

    @Transactional(readOnly = true)
    public HomeBannerMediaType getMediaType(BannerScope scope, Long bannerId) {
        return getRequiredBanner(scope, bannerId).getMediaType();
    }

    private void validateOrderPayload(List<HomeBannerEntity> banners, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ids cannot be empty");
        }
        if (banners.size() != ids.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner order payload is invalid");
        }
        Set<Long> existingIds = banners.stream().map(HomeBannerEntity::getId).collect(HashSet::new, Set::add, Set::addAll);
        Set<Long> requestedIds = new HashSet<>(ids);
        if (existingIds.size() != requestedIds.size() || !existingIds.equals(requestedIds)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "banner order payload is invalid");
        }
    }

    private AdminHomeBannerResponse toAdminResponse(HomeBannerEntity entity) {
        BannerScope scope = resolveScope(entity);
        return new AdminHomeBannerResponse(
                entity.getId(),
                entity.getMediaType().toApiValue(),
                entity.getTitle(),
                entity.getDescription(),
                hasMedia(entity) ? buildContentPath(scope, entity.getId()) : null,
                entity.getPosterPath() != null && !entity.getPosterPath().isBlank() ? buildPosterPath(scope, entity.getId()) : null,
                entity.getSortOrder(),
                hasMedia(entity)
        );
    }

    private PublicHomeBannerItemResponse toPublicResponse(HomeBannerEntity entity) {
        BannerScope scope = resolveScope(entity);
        return new PublicHomeBannerItemResponse(
                entity.getId(),
                entity.getMediaType().toApiValue(),
                entity.getTitle(),
                entity.getDescription(),
                buildContentPath(scope, entity.getId()),
                entity.getPosterPath() != null && !entity.getPosterPath().isBlank() ? buildPosterPath(scope, entity.getId()) : null
        );
    }

    private String buildContentPath(BannerScope scope, Long bannerId) {
        return scope.publicBasePath() + "/" + bannerId + "/content";
    }

    private String buildPosterPath(BannerScope scope, Long bannerId) {
        return scope.publicBasePath() + "/" + bannerId + "/poster";
    }

    private HomeBannerEntity getRequiredBanner(BannerScope scope, Long bannerId) {
        return homeBannerRepository.findByIdAndScope(bannerId, scope)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "home banner not found"));
    }

    private BannerScope resolveScope(HomeBannerEntity entity) {
        return entity.getScope() == null ? BannerScope.HOME : entity.getScope();
    }

    private boolean hasMedia(HomeBannerEntity entity) {
        return entity.getMediaPath() != null && !entity.getMediaPath().isBlank();
    }

    private boolean isAlreadyOptimized(HomeBannerEntity entity, int currentOptimizationVersion) {
        Integer optimizedVersion = entity.getMediaOptimizedVersion();
        return optimizedVersion != null && optimizedVersion >= currentOptimizationVersion;
    }

    private String resolveFailureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "banner recompress failed";
        }
        return message;
    }

    private void clearMedia(HomeBannerEntity entity) {
        entity.setMediaPath(null);
        entity.setMediaContentType(null);
        entity.setMediaSizeBytes(null);
        entity.setMediaOptimizedVersion(null);
        entity.setPosterPath(null);
        entity.setPosterContentType(null);
    }
}
