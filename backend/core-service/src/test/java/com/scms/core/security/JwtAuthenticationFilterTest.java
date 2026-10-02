package com.scms.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void publicNewsPathShouldBypassAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, objectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/news/home");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getContentType());
    }

    @Test
    void recommendedClubsPathShouldBypassAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, objectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/home/recommended-clubs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getContentType());
    }

    @Test
    void activitiesPathShouldBypassAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, objectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/activities");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getContentType());
    }

    @Test
    void esp32TelemetryPathShouldBypassAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, objectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/iot/telemetry");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getContentType());
    }

    @Test
    void cyclingGuardianTelemetryPathShouldBypassAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, objectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/iot/cycling-guardian/telemetry");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getContentType());
    }

    @Test
    void appWorkspacePathShouldRequireAuthentication() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, objectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/clubs/9/app-workspace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }
}
