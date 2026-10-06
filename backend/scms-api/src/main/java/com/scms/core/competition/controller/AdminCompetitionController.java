package com.scms.core.competition.controller;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.competition.domain.CompetitionLeadEntity.LeadStatus;
import com.scms.core.competition.dto.CompetitionImportArticleRequest;
import com.scms.core.competition.dto.CompetitionLeadResponse;
import com.scms.core.competition.dto.CompetitionMutationRequest;
import com.scms.core.competition.dto.CompetitionResponse;
import com.scms.core.competition.dto.CompetitionSourceMutationRequest;
import com.scms.core.competition.dto.CompetitionSourceResponse;
import com.scms.core.competition.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminCompetitionController {

    private final CompetitionService competitionService;
    private final ApiResponseFactory responseFactory;

    public AdminCompetitionController(CompetitionService competitionService,
                                      ApiResponseFactory responseFactory) {
        this.competitionService = competitionService;
        this.responseFactory = responseFactory;
    }

    // ---------------- Sources ----------------

    @GetMapping("/competition-sources")
    public ApiResponse<List<CompetitionSourceResponse>> listSources() {
        return responseFactory.success(competitionService.listSources());
    }

    @PostMapping("/competition-sources")
    public ApiResponse<CompetitionSourceResponse> createSource(
            @Valid @RequestBody CompetitionSourceMutationRequest request) {
        return responseFactory.success(competitionService.createSource(request));
    }

    @PatchMapping("/competition-sources/{sourceId}")
    public ApiResponse<CompetitionSourceResponse> updateSource(
            @PathVariable("sourceId") Long sourceId,
            @Valid @RequestBody CompetitionSourceMutationRequest request) {
        return responseFactory.success(competitionService.updateSource(sourceId, request));
    }

    @DeleteMapping("/competition-sources/{sourceId}")
    public ApiResponse<Void> deleteSource(@PathVariable("sourceId") Long sourceId) {
        competitionService.deleteSource(sourceId);
        return responseFactory.success(null);
    }

    @PostMapping("/competition-sources/{sourceId}/scan")
    public ApiResponse<Map<String, Integer>> scanSource(@PathVariable("sourceId") Long sourceId) {
        int created = competitionService.scanSource(sourceId);
        return responseFactory.success(Map.of("created", created));
    }

    // ---------------- Leads ----------------

    @GetMapping("/competition-leads")
    public ApiResponse<List<CompetitionLeadResponse>> listLeads(
            @RequestParam(name = "status", required = false) LeadStatus status) {
        return responseFactory.success(competitionService.listLeads(status));
    }

    @PostMapping("/competition-leads/{leadId}/confirm")
    public ApiResponse<CompetitionResponse> confirmLead(
            @PathVariable("leadId") Long leadId,
            @Valid @RequestBody CompetitionMutationRequest request) {
        return responseFactory.success(competitionService.confirmLead(leadId, request));
    }

    @PostMapping("/competition-leads/{leadId}/ignore")
    public ApiResponse<Void> ignoreLead(@PathVariable("leadId") Long leadId) {
        competitionService.ignoreLead(leadId);
        return responseFactory.success(null);
    }

    // ---------------- Competitions ----------------

    @GetMapping("/competitions")
    public ApiResponse<List<CompetitionResponse>> listCompetitions() {
        return responseFactory.success(competitionService.listCompetitions());
    }

    @PostMapping("/competitions")
    public ApiResponse<CompetitionResponse> createCompetition(
            @Valid @RequestBody CompetitionMutationRequest request) {
        return responseFactory.success(competitionService.createCompetition(request));
    }

    @PatchMapping("/competitions/{competitionId}")
    public ApiResponse<CompetitionResponse> updateCompetition(
            @PathVariable("competitionId") Long competitionId,
            @Valid @RequestBody CompetitionMutationRequest request) {
        return responseFactory.success(competitionService.updateCompetition(competitionId, request));
    }

    @DeleteMapping("/competitions/{competitionId}")
    public ApiResponse<Void> deleteCompetition(@PathVariable("competitionId") Long competitionId) {
        competitionService.deleteCompetition(competitionId);
        return responseFactory.success(null);
    }

    @PostMapping("/competitions/import-article")
    public ApiResponse<Map<String, Integer>> importArticle(
            @Valid @RequestBody CompetitionImportArticleRequest request) {
        int created = competitionService.importArticle(request);
        return responseFactory.success(Map.of("created", created));
    }
}
