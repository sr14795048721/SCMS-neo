package com.scms.core.clubappworkspace.repository;

import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubAppWorkspaceProjectDemoRuntimeSnapshotRepository
        extends JpaRepository<ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity, Long> {

    Optional<ClubAppWorkspaceProjectDemoRuntimeSnapshotEntity> findFirstByProjectId(Long projectId);
}
