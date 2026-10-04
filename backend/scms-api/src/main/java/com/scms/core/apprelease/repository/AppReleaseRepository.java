package com.scms.core.apprelease.repository;

import com.scms.core.apprelease.domain.AppReleaseEntity;
import com.scms.core.apprelease.domain.AppReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppReleaseRepository extends JpaRepository<AppReleaseEntity, Long> {
    List<AppReleaseEntity> findAllByOrderByCreatedAtDescIdDesc();

    Optional<AppReleaseEntity> findFirstByStatusOrderByPublishedAtDescIdDesc(AppReleaseStatus status);

    boolean existsByVersionNameAndBuildNumberAndIdNot(String versionName, Integer buildNumber, Long id);

    boolean existsByVersionNameAndBuildNumber(String versionName, Integer buildNumber);
}
