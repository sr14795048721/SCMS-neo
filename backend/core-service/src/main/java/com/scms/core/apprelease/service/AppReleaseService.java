package com.scms.core.apprelease.service;

import com.scms.core.apprelease.domain.AppReleaseEntity;
import com.scms.core.apprelease.domain.AppReleasePlatform;
import com.scms.core.apprelease.domain.AppReleaseStatus;
import com.scms.core.apprelease.dto.AdminAppReleaseRequest;
import com.scms.core.apprelease.dto.AdminAppReleaseResponse;
import com.scms.core.apprelease.dto.AppReleaseCheckResponse;
import com.scms.core.apprelease.repository.AppReleaseRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AppReleaseService {

    private final AppReleaseRepository appReleaseRepository;
    private final CurrentUserProvider currentUserProvider;

    public AppReleaseService(AppReleaseRepository appReleaseRepository,
                             CurrentUserProvider currentUserProvider) {
        this.appReleaseRepository = appReleaseRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<AdminAppReleaseResponse> listAdmin() {
        return appReleaseRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminAppReleaseResponse create(AdminAppReleaseRequest request) {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        validateUpsertRequest(request, null);

        AppReleaseEntity entity = new AppReleaseEntity();
        entity.setVersionName(request.versionName().trim());
        entity.setBuildNumber(request.buildNumber());
        entity.setReleaseNotes(request.releaseNotes().trim());
        entity.setForceUpdate(Boolean.TRUE.equals(request.forceUpdate()));
        entity.setAndroidUrl(normalizeUrl(request.androidUrl()));
        entity.setHarmonyUrl(normalizeUrl(request.harmonyUrl()));
        entity.setStatus(AppReleaseStatus.DRAFT);
        entity.setCreatedBy(user.userId());
        entity.setUpdatedBy(user.userId());
        return toAdminResponse(appReleaseRepository.save(entity));
    }

    @Transactional
    public AdminAppReleaseResponse update(Long releaseId, AdminAppReleaseRequest request) {
        AppReleaseEntity entity = getRequiredRelease(releaseId);
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        validateUpsertRequest(request, releaseId);

        entity.setVersionName(request.versionName().trim());
        entity.setBuildNumber(request.buildNumber());
        entity.setReleaseNotes(request.releaseNotes().trim());
        entity.setForceUpdate(Boolean.TRUE.equals(request.forceUpdate()));
        entity.setAndroidUrl(normalizeUrl(request.androidUrl()));
        entity.setHarmonyUrl(normalizeUrl(request.harmonyUrl()));
        entity.setUpdatedBy(user.userId());
        return toAdminResponse(appReleaseRepository.save(entity));
    }

    @Transactional
    public AdminAppReleaseResponse publish(Long releaseId) {
        AppReleaseEntity target = getRequiredRelease(releaseId);
        AuthenticatedUser user = currentUserProvider.getRequiredUser();

        if (target.getReleaseNotes() == null || target.getReleaseNotes().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "release notes are required");
        }
        if (isBlank(target.getAndroidUrl()) && isBlank(target.getHarmonyUrl())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "at least one platform url is required");
        }

        Optional<AppReleaseEntity> currentPublished = appReleaseRepository
                .findFirstByStatusOrderByPublishedAtDescIdDesc(AppReleaseStatus.PUBLISHED);
        if (currentPublished.isPresent() && !currentPublished.get().getId().equals(target.getId())) {
            AppReleaseEntity previous = currentPublished.get();
            previous.setStatus(AppReleaseStatus.RETIRED);
            previous.setUpdatedBy(user.userId());
            appReleaseRepository.save(previous);
        }

        target.setStatus(AppReleaseStatus.PUBLISHED);
        target.setPublishedAt(Instant.now());
        target.setUpdatedBy(user.userId());
        return toAdminResponse(appReleaseRepository.save(target));
    }

    @Transactional
    public AdminAppReleaseResponse retire(Long releaseId) {
        AppReleaseEntity entity = getRequiredRelease(releaseId);
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        entity.setStatus(AppReleaseStatus.RETIRED);
        entity.setUpdatedBy(user.userId());
        return toAdminResponse(appReleaseRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AppReleaseCheckResponse checkLatest(String platformValue, String versionName, Integer buildNumber) {
        AppReleasePlatform platform = AppReleasePlatform.fromValue(platformValue);
        validateVersionName(versionName);
        if (buildNumber == null || buildNumber < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid build number");
        }

        Optional<AppReleaseEntity> published = appReleaseRepository
                .findFirstByStatusOrderByPublishedAtDescIdDesc(AppReleaseStatus.PUBLISHED);
        if (published.isEmpty()) {
            return AppReleaseCheckResponse.noUpdate();
        }

        AppReleaseEntity entity = published.get();
        String downloadUrl = platform == AppReleasePlatform.ANDROID
                ? normalizeUrl(entity.getAndroidUrl())
                : normalizeUrl(entity.getHarmonyUrl());
        if (isBlank(downloadUrl)) {
            return AppReleaseCheckResponse.noUpdate();
        }

        int versionCompare = compareVersionName(entity.getVersionName(), versionName.trim());
        if (versionCompare < 0) {
            return AppReleaseCheckResponse.noUpdate();
        }
        if (versionCompare == 0 && entity.getBuildNumber() <= buildNumber) {
            return AppReleaseCheckResponse.noUpdate();
        }

        return new AppReleaseCheckResponse(
                true,
                Boolean.TRUE.equals(entity.getForceUpdate()),
                entity.getId(),
                entity.getVersionName(),
                entity.getBuildNumber(),
                entity.getReleaseNotes(),
                downloadUrl,
                entity.getPublishedAt()
        );
    }

    private void validateUpsertRequest(AdminAppReleaseRequest request, Long releaseId) {
        String versionName = request.versionName().trim();
        validateVersionName(versionName);

        if (releaseId == null) {
            if (appReleaseRepository.existsByVersionNameAndBuildNumber(versionName, request.buildNumber())) {
                throw new BusinessException(ErrorCode.CONFLICT, "release version already exists");
            }
        } else if (appReleaseRepository.existsByVersionNameAndBuildNumberAndIdNot(versionName, request.buildNumber(), releaseId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "release version already exists");
        }

        validateAbsoluteUrl(request.androidUrl());
        validateAbsoluteUrl(request.harmonyUrl());
    }

    private void validateVersionName(String versionName) {
        if (!versionName.matches("^\\d+\\.\\d+\\.\\d+$")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid version name");
        }
    }

    private void validateAbsoluteUrl(String value) {
        String normalized = normalizeUrl(value);
        if (normalized == null) {
            return;
        }
        try {
            URI uri = new URI(normalized);
            if (!uri.isAbsolute()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "release url must be absolute");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "release url is invalid");
        }
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static int compareVersionName(String left, String right) {
        int[] leftParts = parseVersion(left);
        int[] rightParts = parseVersion(right);
        for (int index = 0; index < 3; index++) {
            int compare = Integer.compare(leftParts[index], rightParts[index]);
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid version name");
        }
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }

    private AppReleaseEntity getRequiredRelease(Long releaseId) {
        return appReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "app release not found"));
    }

    private AdminAppReleaseResponse toAdminResponse(AppReleaseEntity entity) {
        return new AdminAppReleaseResponse(
                entity.getId(),
                entity.getVersionName(),
                entity.getBuildNumber(),
                entity.getReleaseNotes(),
                entity.getForceUpdate(),
                entity.getAndroidUrl(),
                entity.getHarmonyUrl(),
                entity.getStatus().name(),
                entity.getPublishedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
