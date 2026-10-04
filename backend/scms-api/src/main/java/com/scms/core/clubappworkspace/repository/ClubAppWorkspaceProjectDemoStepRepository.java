package com.scms.core.clubappworkspace.repository;

import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ClubAppWorkspaceProjectDemoStepRepository
        extends JpaRepository<ClubAppWorkspaceProjectDemoStepEntity, Long> {

    List<ClubAppWorkspaceProjectDemoStepEntity> findAllByDemoProfileIdOrderBySortOrderAscIdAsc(Long demoProfileId);

    List<ClubAppWorkspaceProjectDemoStepEntity> findAllByDemoProfileIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long demoProfileId);

    List<ClubAppWorkspaceProjectDemoStepEntity> findAllByDemoProfileIdInOrderBySortOrderAscIdAsc(Collection<Long> demoProfileIds);
}
