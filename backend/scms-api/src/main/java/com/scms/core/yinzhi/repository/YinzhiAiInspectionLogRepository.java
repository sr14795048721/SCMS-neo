package com.scms.core.yinzhi.repository;

import com.scms.core.yinzhi.domain.YinzhiAiInspectionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YinzhiAiInspectionLogRepository extends JpaRepository<YinzhiAiInspectionLogEntity, Long> {

    List<YinzhiAiInspectionLogEntity> findAllByRunIdOrderByCreatedAtAscIdAsc(Long runId);
}
