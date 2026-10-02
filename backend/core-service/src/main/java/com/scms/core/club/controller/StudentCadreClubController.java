package com.scms.core.club.controller;

import com.scms.core.club.dto.ManagerClubMemberResponse;
import com.scms.core.club.dto.StudentCadreClubWorkspaceResponse;
import com.scms.core.club.service.StudentCadreClubService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me/clubs")
@PreAuthorize("hasRole('STUDENT')")
public class StudentCadreClubController {

    private final StudentCadreClubService studentCadreClubService;
    private final ApiResponseFactory responseFactory;

    public StudentCadreClubController(StudentCadreClubService studentCadreClubService,
                                      ApiResponseFactory responseFactory) {
        this.studentCadreClubService = studentCadreClubService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clubId}/cadre")
    public ApiResponse<StudentCadreClubWorkspaceResponse> detail(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(studentCadreClubService.getWorkspace(clubId));
    }

    @GetMapping("/{clubId}/cadre/members")
    public ApiResponse<List<ManagerClubMemberResponse>> members(@PathVariable("clubId") Long clubId) {
        return responseFactory.success(studentCadreClubService.listMembers(clubId));
    }

}
