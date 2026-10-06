package com.scms.core.competition.service;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.competition.domain.CompetitionEntity;
import com.scms.core.competition.domain.CompetitionEntity.CompetitionStatus;
import com.scms.core.competition.domain.CompetitionLeadEntity;
import com.scms.core.competition.domain.CompetitionLeadEntity.LeadStatus;
import com.scms.core.competition.domain.CompetitionSourceEntity;
import com.scms.core.competition.dto.CompetitionImportArticleRequest;
import com.scms.core.competition.dto.CompetitionLeadResponse;
import com.scms.core.competition.dto.CompetitionMutationRequest;
import com.scms.core.competition.dto.CompetitionResponse;
import com.scms.core.competition.dto.CompetitionSourceMutationRequest;
import com.scms.core.competition.dto.CompetitionSourceResponse;
import com.scms.core.competition.repository.CompetitionLeadRepository;
import com.scms.core.competition.repository.CompetitionRepository;
import com.scms.core.competition.repository.CompetitionSourceRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin-facing business layer for competition tracking: manages watched
 * sources, pending leads and the competition library, and exposes manual
 * scan / import entry points.
 */
@Service
public class CompetitionService {

    private final CompetitionSourceRepository sourceRepository;
    private final CompetitionLeadRepository leadRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionScanService scanService;
    private final CurrentUserProvider currentUserProvider;

    public CompetitionService(CompetitionSourceRepository sourceRepository,
                              CompetitionLeadRepository leadRepository,
                              CompetitionRepository competitionRepository,
                              CompetitionScanService scanService,
                              CurrentUserProvider currentUserProvider) {
        this.sourceRepository = sourceRepository;
        this.leadRepository = leadRepository;
        this.competitionRepository = competitionRepository;
        this.scanService = scanService;
        this.currentUserProvider = currentUserProvider;
    }

    // ---------------- Sources ----------------

    @Transactional(readOnly = true)
    public List<CompetitionSourceResponse> listSources() {
        requireAdmin();
        return sourceRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(this::toSourceResponse)
                .toList();
    }

    @Transactional
    public CompetitionSourceResponse createSource(CompetitionSourceMutationRequest request) {
        requireAdmin();
        CompetitionSourceEntity entity = new CompetitionSourceEntity();
        applySourceMutation(entity, request);
        return toSourceResponse(sourceRepository.save(entity));
    }

    @Transactional
    public CompetitionSourceResponse updateSource(Long sourceId, CompetitionSourceMutationRequest request) {
        requireAdmin();
        CompetitionSourceEntity entity = getRequiredSource(sourceId);
        applySourceMutation(entity, request);
        return toSourceResponse(sourceRepository.save(entity));
    }

    @Transactional
    public void deleteSource(Long sourceId) {
        requireAdmin();
        CompetitionSourceEntity entity = getRequiredSource(sourceId);
        if (leadRepository.existsBySourceIdAndUrlAndStatus(sourceId, entity.getUrl(), LeadStatus.PENDING)) {
            throw new BusinessException(ErrorCode.DEPENDENCY_EXISTS,
                    "source still has pending leads; ignore them first");
        }
        sourceRepository.delete(entity);
    }

    // ---------------- Leads ----------------

    @Transactional(readOnly = true)
    public List<CompetitionLeadResponse> listLeads(LeadStatus status) {
        requireAdmin();
        List<CompetitionLeadEntity> leads = status == null
                ? leadRepository.findAllByOrderByDetectedAtDescIdDesc()
                : leadRepository.findAllByStatusOrderByDetectedAtDescIdDesc(status);
        Map<Long, String> sourceNames = sourceNameIndex();
        return leads.stream()
                .map(lead -> toLeadResponse(lead, sourceNames))
                .toList();
    }

    /**
     * Confirms a pending lead and creates the corresponding competition entry.
     */
    @Transactional
    public CompetitionResponse confirmLead(Long leadId, CompetitionMutationRequest request) {
        requireAdmin();
        CompetitionLeadEntity lead = getRequiredLead(leadId);
        if (lead.getStatus() != LeadStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "lead is not pending");
        }

        CompetitionEntity competition = new CompetitionEntity();
        competition.setName(trimmed(request.name()));
        competition.setCategory(request.category());
        competition.setParticipantScope(request.participantScope());
        competition.setApplyDeadline(request.applyDeadline());
        competition.setStartDate(request.startDate());
        competition.setEndDate(request.endDate());
        competition.setUrl(lead.getUrl() != null ? lead.getUrl() : request.url());
        competition.setSourceId(lead.getSourceId());
        competition.setStatus(request.status() != null ? request.status() : CompetitionStatus.UPCOMING);
        competition.setNote(request.note());
        CompetitionEntity saved = competitionRepository.save(competition);

        lead.setStatus(LeadStatus.CONFIRMED);
        leadRepository.save(lead);
        return toCompetitionResponse(saved, sourceNameIndex());
    }

    @Transactional
    public void ignoreLead(Long leadId) {
        requireAdmin();
        CompetitionLeadEntity lead = getRequiredLead(leadId);
        if (lead.getStatus() != LeadStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "lead is not pending");
        }
        lead.setStatus(LeadStatus.IGNORED);
        leadRepository.save(lead);
    }

    // ---------------- Competitions ----------------

    @Transactional(readOnly = true)
    public List<CompetitionResponse> listCompetitions() {
        requireAdmin();
        Map<Long, String> sourceNames = sourceNameIndex();
        return competitionRepository.findAllByOrderByApplyDeadlineAscIdDesc()
                .stream()
                .map(competition -> toCompetitionResponse(competition, sourceNames))
                .toList();
    }

    @Transactional
    public CompetitionResponse createCompetition(CompetitionMutationRequest request) {
        requireAdmin();
        CompetitionEntity entity = new CompetitionEntity();
        applyCompetitionMutation(entity, request);
        return toCompetitionResponse(competitionRepository.save(entity), sourceNameIndex());
    }

    @Transactional
    public CompetitionResponse updateCompetition(Long competitionId, CompetitionMutationRequest request) {
        requireAdmin();
        CompetitionEntity entity = getRequiredCompetition(competitionId);
        applyCompetitionMutation(entity, request);
        return toCompetitionResponse(competitionRepository.save(entity), sourceNameIndex());
    }

    @Transactional
    public void deleteCompetition(Long competitionId) {
        requireAdmin();
        CompetitionEntity entity = getRequiredCompetition(competitionId);
        competitionRepository.delete(entity);
    }

    // ---------------- Scan / import ----------------

    /**
     * Manually scans a single source; returns how many new leads were created.
     */
    @Transactional
    public int scanSource(Long sourceId) {
        requireAdmin();
        CompetitionSourceEntity source = getRequiredSource(sourceId);
        return scanService.scanSource(source);
    }

    /**
     * Scans every enabled source that has scan enabled. No admin context is
     * required (used by the scheduled job). Returns total new leads.
     */
    @Transactional
    public int scanAllEnabledSources() {
        int total = 0;
        for (CompetitionSourceEntity source : sourceRepository.findAllByEnabledTrueAndScanEnabledTrue()) {
            try {
                total += scanService.scanSource(source);
            } catch (Exception ignored) {
                // one failing source must not break the whole run
            }
        }
        return total;
    }

    /**
     * Imports a manually pasted article (wechat official account etc).
     */
    @Transactional
    public int importArticle(CompetitionImportArticleRequest request) {
        requireAdmin();
        return scanService.importArticle(request.title(), request.content());
    }

    // ---------------- Helpers ----------------

    private void applySourceMutation(CompetitionSourceEntity entity, CompetitionSourceMutationRequest request) {
        entity.setName(trimmed(request.name()));
        entity.setUrl(request.url());
        entity.setSourceType(request.sourceType() != null ? request.sourceType() : CompetitionSourceEntity.SourceType.OFFICIAL_WEBSITE);
        entity.setWechatName(request.wechatName());
        entity.setEnabled(request.enabled());
        entity.setScanEnabled(request.scanEnabled());
    }

    private void applyCompetitionMutation(CompetitionEntity entity, CompetitionMutationRequest request) {
        entity.setName(trimmed(request.name()));
        entity.setCategory(request.category());
        entity.setParticipantScope(request.participantScope());
        entity.setApplyDeadline(request.applyDeadline());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setUrl(request.url());
        entity.setSourceId(request.sourceId());
        entity.setStatus(request.status() != null ? request.status() : CompetitionStatus.UPCOMING);
        entity.setNote(request.note());
    }

    private Map<Long, String> sourceNameIndex() {
        return sourceRepository.findAll().stream()
                .collect(Collectors.toMap(CompetitionSourceEntity::getId, CompetitionSourceEntity::getName, (a, b) -> a));
    }

    private CompetitionSourceEntity getRequiredSource(Long sourceId) {
        return sourceRepository.findById(sourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "competition source not found"));
    }

    private CompetitionLeadEntity getRequiredLead(Long leadId) {
        return leadRepository.findById(leadId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "competition lead not found"));
    }

    private CompetitionEntity getRequiredCompetition(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "competition not found"));
    }

    private CompetitionSourceResponse toSourceResponse(CompetitionSourceEntity entity) {
        return new CompetitionSourceResponse(
                entity.getId(),
                entity.getName(),
                entity.getUrl(),
                entity.getSourceType(),
                entity.getWechatName(),
                entity.isEnabled(),
                entity.isScanEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CompetitionLeadResponse toLeadResponse(CompetitionLeadEntity lead, Map<Long, String> sourceNames) {
        return new CompetitionLeadResponse(
                lead.getId(),
                lead.getSourceId(),
                lead.getSourceId() == null ? null : sourceNames.get(lead.getSourceId()),
                lead.getTitle(),
                lead.getUrl(),
                lead.getSnippet(),
                lead.getDetectedAt(),
                lead.getStatus(),
                lead.getCreatedAt()
        );
    }

    private CompetitionResponse toCompetitionResponse(CompetitionEntity entity, Map<Long, String> sourceNames) {
        return new CompetitionResponse(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getParticipantScope(),
                entity.getApplyDeadline(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getUrl(),
                entity.getSourceId(),
                entity.getSourceId() == null ? null : sourceNames.get(entity.getSourceId()),
                entity.getStatus(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser currentUser = currentUserProvider.getRequiredUser();
        if (!currentUser.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return currentUser;
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
