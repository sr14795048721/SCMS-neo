package com.scms.core.clubappworkspace.repository;

import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubAppWorkspaceProjectDemoProfileRepository
        extends JpaRepository<ClubAppWorkspaceProjectDemoProfileEntity, Long> {

    Optional<ClubAppWorkspaceProjectDemoProfileEntity> findFirstByProjectId(Long projectId);

    Optional<ClubAppWorkspaceProjectDemoProfileEntity> findFirstByProjectIdAndEnabledTrue(Long projectId);

    List<ClubAppWorkspaceProjectDemoProfileEntity> findAllByProjectIdIn(Collection<Long> projectIds);
}
