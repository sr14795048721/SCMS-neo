package com.scms.core.competition.repository;

import com.scms.core.competition.domain.CompetitionLeadEntity;
import com.scms.core.competition.domain.CompetitionLeadEntity.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionLeadRepository extends JpaRepository<CompetitionLeadEntity, Long> {
    List<CompetitionLeadEntity> findAllByOrderByDetectedAtDescIdDesc();

    List<CompetitionLeadEntity> findAllByStatusOrderByDetectedAtDescIdDesc(LeadStatus status);

    boolean existsBySourceIdAndUrlAndStatus(Long sourceId, String url, LeadStatus status);

    boolean existsByTitleAndStatus(String title, LeadStatus status);
}
