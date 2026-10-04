package com.scms.core.clubappworkspace.repository;

import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubAppWorkspaceProjectRepository extends JpaRepository<ClubAppWorkspaceProjectEntity, Long> {
    List<ClubAppWorkspaceProjectEntity> findAllByGroupIdInOrderBySortOrderAscIdAsc(Collection<Long> groupIds);

    List<ClubAppWorkspaceProjectEntity> findAllByGroupIdInAndEnabledTrueOrderBySortOrderAscIdAsc(Collection<Long> groupIds);

    Optional<ClubAppWorkspaceProjectEntity> findFirstByGroupIdInAndProjectKeyAndEnabledTrue(Collection<Long> groupIds, String projectKey);

    Optional<ClubAppWorkspaceProjectEntity> findFirstByGroupIdInAndProjectKey(Collection<Long> groupIds, String projectKey);

    Optional<ClubAppWorkspaceProjectEntity> findFirstByProjectKey(String projectKey);
}
