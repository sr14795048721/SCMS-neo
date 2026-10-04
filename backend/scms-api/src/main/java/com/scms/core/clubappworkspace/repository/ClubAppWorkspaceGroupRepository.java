package com.scms.core.clubappworkspace.repository;

import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubAppWorkspaceGroupRepository extends JpaRepository<ClubAppWorkspaceGroupEntity, Long> {
    List<ClubAppWorkspaceGroupEntity> findAllByClubIdOrderBySortOrderAscIdAsc(Long clubId);

    List<ClubAppWorkspaceGroupEntity> findAllByClubIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long clubId);
}
