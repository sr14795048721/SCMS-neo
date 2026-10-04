package com.scms.core.score.repository;

import com.scms.core.score.domain.ScoreRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreRuleRepository extends JpaRepository<ScoreRuleEntity, Long> {
    List<ScoreRuleEntity> findAllByOrderByCreatedAtDescIdDesc();

    void deleteAllByClubId(Long clubId);
}
