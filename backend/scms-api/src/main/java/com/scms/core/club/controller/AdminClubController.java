package com.scms.core.club.controller;

import com.scms.core.adminuser.dto.AdminUserPageResponse;
import com.scms.core.club.dto.AdminClubOptionResponse;
import com.scms.core.club.dto.ClubDutyMutationRequest;
import com.scms.core.club.dto.ClubDutyResponse;
import com.scms.core.club.dto.AdminClubDetailResponse;
import com.scms.core.club.dto.AdminClubListItemResponse;
import com.scms.core.club.dto.AdminClubMutationRequest;
import com.scms.core.club.dto.ClubMemberDutyUpdateRequest;
import com.scms.core.club.service.ClubDutyManagementService;
import com.scms.core.club.service.AdminClubService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
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

@RestController
@RequestMapping("/api/v1/admin/clubs")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminClubController {

    private final AdminClubService adminClubService;
    private final ClubDutyManagementService clubDutyManagementService;
    private final ApiResponseFactory responseFactory;

    public AdminClubController(AdminClubService adminClubService,
                               ClubDutyManagementService clubDutyManagementService,
                               ApiResponseFactory responseFactory) {
        this.adminClubService = adminClubService;
        this.clubDutyManagementService = clubDutyManagementService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<AdminUserPageResponse<AdminClubListItemResponse>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "status", defaultValue = "") String status
    ) {
        return responseFactory.success(adminClubService.list(page, pageSize, keyword, status));
    }

    @GetMapping("/options")
    public ApiResponse<List<AdminClubOptionResponse>> options(
            @RequestParam(name = "keyword", defaultValue = "") String keyword
    ) {
        return responseFactory.success(adminClubService.listOptions(keyword));
    }

    @PostMapping
    public ApiResponse<AdminClubDetailResponse> create(@Valid @RequestBody AdminClubMutationRequest request) {
        return responseFactory.success(adminClubService.create(request));
    }

    @GetMapping("/{clubId}")
    public ApiResponse<AdminClubDetailResponse> detail(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(adminClubService.getDetail(clubId));
    }

    @PatchMapping("/{clubId}")
    public ApiResponse<AdminClubDetailResponse> update(@PathVariable("clubId") Long clubId,
                                                       @Valid @RequestBody AdminClubMutationRequest request) {
        return responseFactory.success(adminClubService.update(clubId, request));
    }

    @DeleteMapping("/{clubId}")
    public ApiResponse<Void> delete(@PathVariable("clubId") Long clubId) {
        adminClubService.delete(clubId);
        return responseFactory.success(null);
    }

    @GetMapping("/{clubId}/duties")
    public ApiResponse<List<ClubDutyResponse>> listDuties(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(clubDutyManagementService.listAsAdmin(clubId));
    }

    @PostMapping("/{clubId}/duties")
    public ApiResponse<ClubDutyResponse> createDuty(@PathVariable("clubId") Long clubId,
                                                    @Valid @RequestBody ClubDutyMutationRequest request) {
        return responseFactory.success(clubDutyManagementService.createAsAdmin(clubId, request));
    }

    @PatchMapping("/{clubId}/duties/{dutyId}")
    public ApiResponse<ClubDutyResponse> updateDuty(@PathVariable("clubId") Long clubId,
                                                    @PathVariable("dutyId") Long dutyId,
                                                    @Valid @RequestBody ClubDutyMutationRequest request) {
        return responseFactory.success(clubDutyManagementService.updateAsAdmin(clubId, dutyId, request));
    }

    @DeleteMapping("/{clubId}/duties/{dutyId}")
    public ApiResponse<Void> deleteDuty(@PathVariable("clubId") Long clubId,
                                        @PathVariable("dutyId") Long dutyId) {
        clubDutyManagementService.deleteAsAdmin(clubId, dutyId);
        return responseFactory.success(null);
    }

    @PatchMapping("/{clubId}/members/{studentUserId}/duty")
    public ApiResponse<Void> updateMemberDuty(@PathVariable("clubId") Long clubId,
                                              @PathVariable("studentUserId") Long studentUserId,
                                              @Valid @RequestBody ClubMemberDutyUpdateRequest request) {
        clubDutyManagementService.assignMemberDutyAsAdmin(clubId, studentUserId, request.dutyId());
        return responseFactory.success(null);
    }

    @DeleteMapping("/{clubId}/members/{studentUserId}")
    public ApiResponse<Void> removeMember(@PathVariable("clubId") Long clubId,
                                          @PathVariable("studentUserId") Long studentUserId) {
        adminClubService.removeMember(clubId, studentUserId);
        return responseFactory.success(null);
    }
}
