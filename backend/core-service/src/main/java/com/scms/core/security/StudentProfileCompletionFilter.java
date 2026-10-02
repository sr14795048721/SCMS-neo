package com.scms.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.RequestIdUtil;
import com.scms.core.student.service.StudentProfileCompletion;
import com.scms.core.student.service.StudentProfileCompletionService;
import com.scms.core.user.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class StudentProfileCompletionFilter extends OncePerRequestFilter {

    static final int PROFILE_INCOMPLETE_STATUS = 428;
    static final String PROFILE_INCOMPLETE_CODE = "PROFILE_INCOMPLETE";

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/students/me/info",
            "/api/v1/students/me/avatar",
            "/api/v1/students/me/password"
    );

    private final StudentProfileCompletionService studentProfileCompletionService;
    private final ObjectMapper objectMapper;

    public StudentProfileCompletionFilter(StudentProfileCompletionService studentProfileCompletionService,
                                          ObjectMapper objectMapper) {
        this.studentProfileCompletionService = studentProfileCompletionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser user = resolveAuthenticatedUser();
        if (user == null || user.role() != UserRole.STUDENT) {
            filterChain.doFilter(request, response);
            return;
        }

        StudentProfileCompletion completion = studentProfileCompletionService.getCurrentCompletion(user.userId());
        if (completion.completed()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeProfileIncomplete(response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/v1/")) {
            return true;
        }

        return ALLOWED_PATHS.contains(path);
    }

    private AuthenticatedUser resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    private void writeProfileIncomplete(HttpServletResponse response) throws IOException {
        response.setStatus(PROFILE_INCOMPLETE_STATUS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(
                PROFILE_INCOMPLETE_CODE,
                "student profile must be completed before using the system",
                RequestIdUtil.resolveFromMdcOrGenerate()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
