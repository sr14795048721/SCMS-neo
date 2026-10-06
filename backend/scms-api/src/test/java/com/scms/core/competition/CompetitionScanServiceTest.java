package com.scms.core.competition;

import com.scms.core.competition.domain.CompetitionLeadEntity;
import com.scms.core.competition.domain.CompetitionLeadEntity.LeadStatus;
import com.scms.core.competition.repository.CompetitionLeadRepository;
import com.scms.core.competition.service.CompetitionScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitionScanServiceTest {

    private CompetitionLeadRepository leadRepository;
    private CompetitionScanService service;

    @BeforeEach
    void setUp() {
        leadRepository = mock(CompetitionLeadRepository.class);
        service = new CompetitionScanService(leadRepository);
    }

    @Test
    void importArticleShouldCreatePendingLead() {
        when(leadRepository.existsByTitleAndStatus("青少年科技创新大赛", LeadStatus.PENDING)).thenReturn(false);

        int result = service.importArticle(
                "青少年科技创新大赛",
                "全国青少年科技创新大赛报名开始，截止日期为12月1日。");

        assertEquals(1, result);
        ArgumentCaptor<CompetitionLeadEntity> captor = ArgumentCaptor.forClass(CompetitionLeadEntity.class);
        verify(leadRepository).save(captor.capture());
        assertEquals("青少年科技创新大赛", captor.getValue().getTitle());
        assertEquals(LeadStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void importArticleShouldSkipDuplicateTitle() {
        when(leadRepository.existsByTitleAndStatus("青少年科技创新大赛", LeadStatus.PENDING)).thenReturn(true);

        int result = service.importArticle(
                "青少年科技创新大赛",
                "全国青少年科技创新大赛报名开始，截止日期为12月1日。");

        assertEquals(0, result);
        verify(leadRepository, never()).save(any());
    }

    @Test
    void importArticleShouldDeriveTitleFromContentWhenMissing() {
        when(leadRepository.existsByTitleAndStatus("全国青少年科技创新大赛通知", LeadStatus.PENDING)).thenReturn(false);

        int result = service.importArticle(null, "全国青少年科技创新大赛通知\n报名截止12月1日，请尽快报名。");

        assertEquals(1, result);
        ArgumentCaptor<CompetitionLeadEntity> captor = ArgumentCaptor.forClass(CompetitionLeadEntity.class);
        verify(leadRepository).save(captor.capture());
        assertEquals("全国青少年科技创新大赛通知", captor.getValue().getTitle());
    }

    @Test
    void importArticleShouldRejectEmptyInput() {
        assertEquals(0, service.importArticle(null, null));
        verify(leadRepository, never()).save(any());
    }
}
