package com.scms.core.competition.repository;

import com.scms.core.competition.domain.CompetitionEntity;
import com.scms.core.competition.domain.CompetitionEntity.CompetitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionRepository extends JpaRepository<CompetitionEntity, Long> {
    List<CompetitionEntity> findAllByOrderByApplyDeadlineAscIdDesc();

    List<CompetitionEntity> findAllByStatus(CompetitionStatus status);
}
