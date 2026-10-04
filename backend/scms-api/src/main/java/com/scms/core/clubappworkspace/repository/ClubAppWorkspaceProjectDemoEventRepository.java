package com.scms.core.clubappworkspace.repository;

import com.scms.core.clubappworkspace.domain.ClubAppWorkspaceProjectDemoEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubAppWorkspaceProjectDemoEventRepository
        extends JpaRepository<ClubAppWorkspaceProjectDemoEventEntity, Long> {

    List<ClubAppWorkspaceProjectDemoEventEntity> findTop12ByProjectIdOrderByEventAtDescIdDesc(Long projectId);

    List<ClubAppWorkspaceProjectDemoEventEntity> findAllByProjectIdOrderByEventAtDescIdDesc(Long projectId);
}
