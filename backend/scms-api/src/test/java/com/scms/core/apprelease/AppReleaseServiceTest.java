package com.scms.core.apprelease;

import com.scms.core.apprelease.domain.AppReleaseEntity;
import com.scms.core.apprelease.domain.AppReleaseStatus;
import com.scms.core.apprelease.dto.AdminAppReleaseRequest;
import com.scms.core.apprelease.repository.AppReleaseRepository;
import com.scms.core.apprelease.service.AppReleaseService;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppReleaseServiceTest {

    private AppReleaseRepository appReleaseRepository;
    private CurrentUserProvider currentUserProvider;
    private AppReleaseService appReleaseService;

    @BeforeEach
    void setUp() {
        appReleaseRepository = mock(AppReleaseRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        appReleaseService = new AppReleaseService(appReleaseRepository, currentUserProvider);

        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(9L, "admin", UserRole.ADMIN));
    }

    @Test
    void createShouldPersistDraftRelease() {
        when(appReleaseRepository.existsByVersionNameAndBuildNumber("1.2.3", 5)).thenReturn(false);
        when(appReleaseRepository.save(any(AppReleaseEntity.class))).thenAnswer(invocation -> {
            AppReleaseEntity entity = invocation.getArgument(0);
            setField(entity, "id", 12L);
            return entity;
        });

        var response = appReleaseService.create(new AdminAppReleaseRequest(
                "1.2.3",
                5,
                "notes",
                false,
                "https://example.com/android.apk",
                "https://example.com/harmony.hap"
        ));

        assertEquals(12L, response.id());
        assertEquals("DRAFT", response.status());
        assertEquals(9L, response.createdBy());
    }

    @Test
    void publishShouldRetirePreviousPublishedRelease() {
        AppReleaseEntity current = release(1L, "1.0.0", 1, AppReleaseStatus.PUBLISHED);
        AppReleaseEntity next = release(2L, "1.0.1", 2, AppReleaseStatus.DRAFT);
        next.setAndroidUrl("https://example.com/android.apk");

        when(appReleaseRepository.findById(2L)).thenReturn(Optional.of(next));
        when(appReleaseRepository.findFirstByStatusOrderByPublishedAtDescIdDesc(AppReleaseStatus.PUBLISHED))
                .thenReturn(Optional.of(current));
        when(appReleaseRepository.save(any(AppReleaseEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = appReleaseService.publish(2L);

        assertEquals("PUBLISHED", response.status());
        assertEquals(AppReleaseStatus.RETIRED, current.getStatus());
    }

    @Test
    void checkLatestShouldReturnNoUpdateWhenPlatformUrlMissing() {
        AppReleaseEntity published = release(5L, "1.0.1", 2, AppReleaseStatus.PUBLISHED);
        published.setAndroidUrl(null);
        published.setHarmonyUrl("https://example.com/harmony.hap");

        when(appReleaseRepository.findFirstByStatusOrderByPublishedAtDescIdDesc(AppReleaseStatus.PUBLISHED))
                .thenReturn(Optional.of(published));

        var response = appReleaseService.checkLatest("ANDROID", "1.0.0", 1);

        assertFalse(response.hasUpdate());
    }

    @Test
    void checkLatestShouldCompareBuildNumberWhenVersionMatches() {
        AppReleaseEntity published = release(8L, "1.0.0", 5, AppReleaseStatus.PUBLISHED);
        published.setAndroidUrl("https://example.com/android.apk");

        when(appReleaseRepository.findFirstByStatusOrderByPublishedAtDescIdDesc(AppReleaseStatus.PUBLISHED))
                .thenReturn(Optional.of(published));

        var response = appReleaseService.checkLatest("ANDROID", "1.0.0", 4);

        assertEquals(true, response.hasUpdate());
        assertEquals("https://example.com/android.apk", response.downloadUrl());
    }

    @Test
    void publishShouldRejectReleaseWithoutAnyPlatformUrl() {
        AppReleaseEntity draft = release(3L, "1.0.1", 2, AppReleaseStatus.DRAFT);
        when(appReleaseRepository.findById(3L)).thenReturn(Optional.of(draft));

        assertThrows(BusinessException.class, () -> appReleaseService.publish(3L));
        verify(appReleaseRepository, never()).save(any(AppReleaseEntity.class));
    }

    private AppReleaseEntity release(Long id, String versionName, Integer buildNumber, AppReleaseStatus status) {
        AppReleaseEntity entity = new AppReleaseEntity();
        setField(entity, "id", id);
        setField(entity, "createdAt", Instant.parse("2026-04-01T08:00:00Z"));
        setField(entity, "updatedAt", Instant.parse("2026-04-01T08:00:00Z"));
        entity.setVersionName(versionName);
        entity.setBuildNumber(buildNumber);
        entity.setReleaseNotes("notes");
        entity.setForceUpdate(false);
        entity.setStatus(status);
        entity.setCreatedBy(9L);
        entity.setUpdatedBy(9L);
        entity.setPublishedAt(Instant.parse("2026-04-01T08:00:00Z"));
        return entity;
    }

    private void setField(AppReleaseEntity entity, String name, Object value) {
        try {
            var field = AppReleaseEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
