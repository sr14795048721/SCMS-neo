package com.scms.core.changelog.repository;

import com.scms.core.changelog.domain.VersionChangelogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionChangelogRepository extends JpaRepository<VersionChangelogEntity, Long> {
    List<VersionChangelogEntity> findAllByOrderByReleasedAtDescIdDesc();

    boolean existsByVersion(String version);
}
