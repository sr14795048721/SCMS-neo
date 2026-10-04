package com.scms.core.clubappworkspace.repository;

import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectMaterialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubAppWorkspaceProjectMaterialRepository extends JpaRepository<ClubAppWorkspaceProjectMaterialEntity, Long> {
    List<ClubAppWorkspaceProjectMaterialEntity> findAllByProjectIdInOrderBySortOrderAscIdAsc(Collection<Long> projectIds);

    List<ClubAppWorkspaceProjectMaterialEntity> findAllByProjectIdInAndEnabledTrueOrderBySortOrderAscIdAsc(Collection<Long> projectIds);

    List<ClubAppWorkspaceProjectMaterialEntity> findAllByProjectIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long projectId);

    Optional<ClubAppWorkspaceProjectMaterialEntity> findFirstByIdAndProjectIdAndEnabledTrue(Long id, Long projectId);
}
