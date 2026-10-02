package com.scms.core.club.controller;

import com.scms.core.club.dto.ManagerClubDetailResponse;
import com.scms.core.club.dto.ManagerClubJoinRequestResponse;
import com.scms.core.club.dto.ManagerClubMemberResponse;
import com.scms.core.club.dto.ClubDutyMutationRequest;
import com.scms.core.club.dto.ClubDutyResponse;
import com.scms.core.club.dto.ClubMemberDutyUpdateRequest;
import com.scms.core.club.dto.ManagerClubSummaryResponse;
import com.scms.core.club.service.ClubDutyManagementService;
import com.scms.core.club.service.ManagerClubService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers/me/clubs")
@PreAuthorize("hasRole('CLUB_MANAGER')")
public class ManagerClubController {

    private final ManagerClubService managerClubService;
    private final ClubDutyManagementService clubDutyManagementService;
    private final ApiResponseFactory responseFactory;

    public ManagerClubController(ManagerClubService managerClubService,
                                 ClubDutyManagementService clubDutyManagementService,
                                 ApiResponseFactory responseFactory) {
        this.managerClubService = managerClubService;
        this.clubDutyManagementService = clubDutyManagementService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<ManagerClubSummaryResponse>> listClubs() {
        return responseFactory.success(managerClubService.listManagedClubs());
    }

    @GetMapping("/{clubId}")
    public ApiResponse<ManagerClubDetailResponse> detail(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(managerClubService.getManagedClubDetail(clubId));
    }

    @GetMapping("/{clubId}/members")
    public ApiResponse<List<ManagerClubMemberResponse>> listMembers(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(managerClubService.listMembers(clubId));
    }

    @GetMapping("/{clubId}/join-requests")
    public ApiResponse<List<ManagerClubJoinRequestResponse>> listJoinRequests(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(managerClubService.listJoinRequests(clubId));
    }

    @PostMapping("/{clubId}/join-requests/{requestId}/approve")
    public ApiResponse<ManagerClubJoinRequestResponse> approveJoinRequest(@PathVariable("clubId") Long clubId,
                                                                          @PathVariable("requestId") Long requestId) {
        return responseFactory.success(managerClubService.approveJoinRequest(clubId, requestId));
    }

    @PostMapping("/{clubId}/join-requests/{requestId}/reject")
    public ApiResponse<ManagerClubJoinRequestResponse> rejectJoinRequest(@PathVariable("clubId") Long clubId,
                                                                         @PathVariable("requestId") Long requestId) {
        return responseFactory.success(managerClubService.rejectJoinRequest(clubId, requestId));
    }

    @GetMapping("/{clubId}/duties")
    public ApiResponse<List<ClubDutyResponse>> listDuties(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(clubDutyManagementService.listAsManager(clubId));
    }

    @PostMapping("/{clubId}/duties")
    public ApiResponse<ClubDutyResponse> createDuty(@PathVariable("clubId") Long clubId,
                                                    @Valid @RequestBody ClubDutyMutationRequest request) {
        return responseFactory.success(clubDutyManagementService.createAsManager(clubId, request));
    }

    @PatchMapping("/{clubId}/duties/{dutyId}")
    public ApiResponse<ClubDutyResponse> updateDuty(@PathVariable("clubId") Long clubId,
                                                    @PathVariable("dutyId") Long dutyId,
                                                    @Valid @RequestBody ClubDutyMutationRequest request) {
        return responseFactory.success(clubDutyManagementService.updateAsManager(clubId, dutyId, request));
    }

    @DeleteMapping("/{clubId}/duties/{dutyId}")
    public ApiResponse<Void> deleteDuty(@PathVariable("clubId") Long clubId,
                                        @PathVariable("dutyId") Long dutyId) {
        clubDutyManagementService.deleteAsManager(clubId, dutyId);
        return responseFactory.success(null);
    }

    @PatchMapping("/{clubId}/members/{studentUserId}/duty")
    public ApiResponse<Void> updateMemberDuty(@PathVariable("clubId") Long clubId,
                                              @PathVariable("studentUserId") Long studentUserId,
                                              @Valid @RequestBody ClubMemberDutyUpdateRequest request) {
        clubDutyManagementService.assignMemberDutyAsManager(clubId, studentUserId, request.dutyId());
        return responseFactory.success(null);
    }

    @DeleteMapping("/{clubId}/members/{studentUserId}")
    public ApiResponse<Void> removeMember(@PathVariable("clubId") Long clubId,
                                          @PathVariable("studentUserId") Long studentUserId) {
        managerClubService.removeMember(clubId, studentUserId);
        return responseFactory.success(null);
    }
}
