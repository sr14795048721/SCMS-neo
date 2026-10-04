package com.scms.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scms.core.student.service.StudentProfileCompletion;
import com.scms.core.student.service.StudentProfileCompletionService;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentProfileCompletionFilterTest {

    private final StudentProfileCompletionService studentProfileCompletionService = mock(StudentProfileCompletionService.class);
    private final StudentProfileCompletionFilter filter = new StudentProfileCompletionFilter(
            studentProfileCompletionService,
            new ObjectMapper().registerModule(new JavaTimeModule())
    );

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBypassAllowedStudentProfilePaths() throws Exception {
        authenticateAsStudent();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students/me/info");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(studentProfileCompletionService, never()).getCurrentCompletion(9L);
    }

    @Test
    void shouldRejectIncompleteStudentProfileOnProtectedPath() throws Exception {
        authenticateAsStudent();
        when(studentProfileCompletionService.getCurrentCompletion(9L))
                .thenReturn(new StudentProfileCompletion(false, List.of("studentNo", "className")));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students/me/discover-clubs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(StudentProfileCompletionFilter.PROFILE_INCOMPLETE_STATUS, response.getStatus());
        assertTrue(response.getContentAsString().contains(StudentProfileCompletionFilter.PROFILE_INCOMPLETE_CODE));
    }

    @Test
    void shouldAllowCompletedStudentProfileOnProtectedPath() throws Exception {
        authenticateAsStudent();
        when(studentProfileCompletionService.getCurrentCompletion(9L))
                .thenReturn(new StudentProfileCompletion(true, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students/me/discover-clubs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(studentProfileCompletionService).getCurrentCompletion(9L);
    }

    private void authenticateAsStudent() {
        AuthenticatedUser principal = new AuthenticatedUser(9L, "student", UserRole.STUDENT);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
