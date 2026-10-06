package com.scms.core.competition;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.competition.domain.CompetitionEntity;
import com.scms.core.competition.domain.CompetitionEntity.CompetitionStatus;
import com.scms.core.competition.domain.CompetitionLeadEntity;
import com.scms.core.competition.domain.CompetitionLeadEntity.LeadStatus;
import com.scms.core.competition.domain.CompetitionSourceEntity;
import com.scms.core.competition.dto.CompetitionImportArticleRequest;
import com.scms.core.competition.dto.CompetitionMutationRequest;
import com.scms.core.competition.dto.CompetitionSourceMutationRequest;
import com.scms.core.competition.repository.CompetitionLeadRepository;
import com.scms.core.competition.repository.CompetitionRepository;
import com.scms.core.competition.repository.CompetitionSourceRepository;
import com.scms.core.competition.service.CompetitionScanService;
import com.scms.core.competition.service.CompetitionService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitionServiceTest {

    private CompetitionSourceRepository sourceRepository;
    private CompetitionLeadRepository leadRepository;
    private CompetitionRepository competitionRepository;
    private CompetitionScanService scanService;
    private CurrentUserProvider currentUserProvider;
    private CompetitionService service;

    @BeforeEach
    void setUp() {
        sourceRepository = mock(CompetitionSourceRepository.class);
        leadRepository = mock(CompetitionLeadRepository.class);
        competitionRepository = mock(CompetitionRepository.class);
        scanService = mock(CompetitionScanService.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        service = new CompetitionService(
                sourceRepository, leadRepository, competitionRepository, scanService, currentUserProvider);
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
    }

    @Test
    void nonAdminShouldBeRejected() {
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(3L, "manager", UserRole.CLUB_MANAGER));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.listSources());

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void createSourceShouldPersistEntity() {
        when(sourceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createSource(new CompetitionSourceMutationRequest(
                "全国青少年科技创新大赛官网", "https://example.com", null, null, true, true));

        assertEquals("全国青少年科技创新大赛官网", response.name());
        verify(sourceRepository).save(any(CompetitionSourceEntity.class));
    }

    @Test
    void confirmLeadShouldCreateCompetitionAndMarkLeadConfirmed() {
        CompetitionLeadEntity lead = new CompetitionLeadEntity();
        setId(lead, 5L);
        lead.setTitle("青少年科技创新大赛");
        lead.setUrl("https://example.com/notice");
        lead.setSourceId(7L);
        lead.setStatus(LeadStatus.PENDING);
        when(leadRepository.findById(5L)).thenReturn(Optional.of(lead));
        when(competitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sourceRepository.findAll()).thenReturn(List.of());

        var response = service.confirmLead(5L, new CompetitionMutationRequest(
                "青少年科技创新大赛", "科技创新", "高中生", null, null, null,
                null, 7L, CompetitionStatus.UPCOMING, "注意截止时间"));

        assertEquals("青少年科技创新大赛", response.name());
        assertEquals("高中生", response.participantScope());
        assertEquals("https://example.com/notice", response.url());
        assertEquals(LeadStatus.CONFIRMED, lead.getStatus());
        verify(leadRepository).save(lead);
    }

    @Test
    void confirmLeadShouldRejectAlreadyHandledLead() {
        CompetitionLeadEntity lead = new CompetitionLeadEntity();
        setId(lead, 5L);
        lead.setStatus(LeadStatus.IGNORED);
        when(leadRepository.findById(5L)).thenReturn(Optional.of(lead));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.confirmLead(5L, new CompetitionMutationRequest(
                        "青少年科技创新大赛", null, null, null, null, null,
                        null, null, null, null))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void deleteSourceShouldRejectWithPendingLeads() {
        CompetitionSourceEntity source = new CompetitionSourceEntity();
        setId(source, 3L);
        source.setUrl("https://example.com");
        when(sourceRepository.findById(3L)).thenReturn(Optional.of(source));
        when(leadRepository.existsBySourceIdAndUrlAndStatus(3L, "https://example.com", LeadStatus.PENDING))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteSource(3L)
        );

        assertEquals(ErrorCode.DEPENDENCY_EXISTS, exception.getErrorCode());
        verify(sourceRepository, never()).delete(any());
    }

    @Test
    void importArticleShouldDelegateToScanService() {
        when(scanService.importArticle("青少年编程大赛", "报名开始")).thenReturn(1);

        int created = service.importArticle(new CompetitionImportArticleRequest(
                "https://example.com/a", "青少年编程大赛", "报名开始"));

        assertEquals(1, created);
        verify(scanService).importArticle("青少年编程大赛", "报名开始");
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
