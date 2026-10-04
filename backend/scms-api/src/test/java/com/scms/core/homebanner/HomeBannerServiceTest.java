package com.scms.core.homebanner;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.homebanner.domain.BannerScope;
import com.scms.core.homebanner.domain.HomeBannerEntity;
import com.scms.core.homebanner.domain.HomeBannerMediaType;
import com.scms.core.homebanner.repository.HomeBannerRepository;
import com.scms.core.homebanner.service.BannerMediaStorageService;
import com.scms.core.homebanner.service.HomeBannerService;
import com.scms.core.homebanner.service.StoredBannerMedia;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeBannerServiceTest {

    private HomeBannerRepository homeBannerRepository;
    private CurrentUserProvider currentUserProvider;
    private BannerMediaStorageService bannerMediaStorageService;
    private HomeBannerService homeBannerService;

    @BeforeEach
    void setUp() {
        homeBannerRepository = mock(HomeBannerRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        bannerMediaStorageService = mock(BannerMediaStorageService.class);
        homeBannerService = new HomeBannerService(
                homeBannerRepository,
                currentUserProvider,
                bannerMediaStorageService
        );

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(7L, "admin", UserRole.ADMIN));
    }

    @Test
    void uploadFileShouldPersistOptimizedVersionFromStorage() {
        HomeBannerEntity entity = imageBanner(5L, "images/old.jpg", 1);
        MultipartFile file = mock(MultipartFile.class);

        when(homeBannerRepository.findByIdAndScope(5L, BannerScope.HOME)).thenReturn(Optional.of(entity));
        when(bannerMediaStorageService.store(HomeBannerMediaType.IMAGE, file)).thenReturn(
                new StoredBannerMedia("images/new.jpg", "image/jpeg", 512L, 3, null, null)
        );
        when(homeBannerRepository.save(any(HomeBannerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        homeBannerService.uploadFile(BannerScope.HOME, 5L, file);

        assertEquals("images/new.jpg", entity.getMediaPath());
        assertEquals("image/jpeg", entity.getMediaContentType());
        assertEquals(512L, entity.getMediaSizeBytes());
        assertEquals(3, entity.getMediaOptimizedVersion());
    }

    @Test
    void recompressShouldProcessOnlyPendingImageBanners() {
        HomeBannerEntity pendingImage = imageBanner(1L, "images/1.jpg", null);
        HomeBannerEntity optimizedImage = imageBanner(2L, "images/2.jpg", 2);
        HomeBannerEntity videoBanner = videoBanner(3L, "videos/3.mp4");
        HomeBannerEntity emptyImage = imageBanner(4L, null, null);

        when(homeBannerRepository.findAllByScopeOrderBySortOrderAscIdAsc(BannerScope.HOME))
                .thenReturn(List.of(pendingImage, optimizedImage, videoBanner, emptyImage));
        when(homeBannerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bannerMediaStorageService.getBannerOptimizationVersion()).thenReturn(2);
        when(bannerMediaStorageService.recompressImage("images/1.jpg", "image/jpeg")).thenReturn(
                new StoredBannerMedia("images/1.jpg", "image/jpeg", 640L, 2, null, null)
        );

        var response = homeBannerService.recompress(BannerScope.HOME);

        assertEquals(1, response.processedCount());
        assertEquals(3, response.skippedCount());
        assertEquals(0, response.failedCount());
        assertTrue(response.failures().isEmpty());
        assertEquals(640L, pendingImage.getMediaSizeBytes());
        assertEquals(2, pendingImage.getMediaOptimizedVersion());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HomeBannerEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(homeBannerRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(1L, captor.getValue().get(0).getId());
    }

    @Test
    void recompressShouldContinueWhenOneBannerFails() {
        HomeBannerEntity brokenImage = imageBanner(11L, "images/broken.jpg", null);
        HomeBannerEntity okImage = imageBanner(12L, "images/ok.jpg", null);

        when(homeBannerRepository.findAllByScopeOrderBySortOrderAscIdAsc(BannerScope.LOGIN))
                .thenReturn(List.of(brokenImage, okImage));
        when(homeBannerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bannerMediaStorageService.getBannerOptimizationVersion()).thenReturn(4);
        when(bannerMediaStorageService.recompressImage("images/broken.jpg", "image/jpeg"))
                .thenThrow(new BusinessException(ErrorCode.SYSTEM_ERROR, "optimize failed"));
        when(bannerMediaStorageService.recompressImage("images/ok.jpg", "image/jpeg")).thenReturn(
                new StoredBannerMedia("images/ok.jpg", "image/jpeg", 480L, 4, null, null)
        );

        var response = homeBannerService.recompress(BannerScope.LOGIN);

        assertEquals(1, response.processedCount());
        assertEquals(0, response.skippedCount());
        assertEquals(1, response.failedCount());
        assertEquals(1, response.failures().size());
        assertEquals(11L, response.failures().get(0).bannerId());
        assertEquals("optimize failed", response.failures().get(0).reason());
        assertFalse(okImage.getMediaOptimizedVersion() == null);
    }

    private HomeBannerEntity imageBanner(Long id, String mediaPath, Integer optimizedVersion) {
        HomeBannerEntity entity = new HomeBannerEntity();
        setField(entity, "id", id);
        entity.setScope(BannerScope.HOME);
        entity.setTitle("banner");
        entity.setDescription("");
        entity.setMediaType(HomeBannerMediaType.IMAGE);
        entity.setMediaPath(mediaPath);
        entity.setMediaContentType("image/jpeg");
        entity.setMediaSizeBytes(1024L);
        entity.setMediaOptimizedVersion(optimizedVersion);
        entity.setSortOrder(id.intValue());
        entity.setCreatedBy(7L);
        entity.setUpdatedBy(7L);
        return entity;
    }

    private HomeBannerEntity videoBanner(Long id, String mediaPath) {
        HomeBannerEntity entity = new HomeBannerEntity();
        setField(entity, "id", id);
        entity.setScope(BannerScope.HOME);
        entity.setTitle("video");
        entity.setDescription("");
        entity.setMediaType(HomeBannerMediaType.VIDEO);
        entity.setMediaPath(mediaPath);
        entity.setMediaContentType("video/mp4");
        entity.setMediaSizeBytes(2048L);
        entity.setSortOrder(id.intValue());
        entity.setCreatedBy(7L);
        entity.setUpdatedBy(7L);
        return entity;
    }

    private void setField(HomeBannerEntity entity, String name, Object value) {
        try {
            var field = HomeBannerEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
