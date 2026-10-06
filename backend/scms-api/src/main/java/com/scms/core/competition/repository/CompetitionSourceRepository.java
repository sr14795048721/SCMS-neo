package com.scms.core.competition.repository;

import com.scms.core.competition.domain.CompetitionSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionSourceRepository extends JpaRepository<CompetitionSourceEntity, Long> {
    List<CompetitionSourceEntity> findAllByOrderByCreatedAtDescIdDesc();

    List<CompetitionSourceEntity> findAllByEnabledTrueAndScanEnabledTrue();
}
