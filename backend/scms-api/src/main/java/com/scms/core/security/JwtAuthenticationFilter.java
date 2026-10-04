package com.scms.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.web.RequestIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (PublicApiRequestMatchers.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = JwtService.extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            writeUnauthorized(response, "missing bearer token");
            return;
        }

        try {
            JwtTokenClaims claims = jwtService.parseAccessToken(token);
            AuthenticatedUser principal = new AuthenticatedUser(claims.userId(), claims.username(), claims.role());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, exception.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.UNAUTHORIZED.name(),
                message,
                RequestIdUtil.resolveFromMdcOrGenerate()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
