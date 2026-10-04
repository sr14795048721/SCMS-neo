package com.scms.core.auth.controller;

import com.scms.core.auth.dto.LoginRequest;
import com.scms.core.auth.dto.LogoutRequest;
import com.scms.core.auth.dto.RefreshTokenRequest;
import com.scms.core.auth.dto.RegisterRequest;
import com.scms.core.auth.dto.TokenResponse;
import com.scms.core.auth.dto.UserMeResponse;
import com.scms.core.auth.service.AuthService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final ApiResponseFactory responseFactory;

    public AuthController(AuthService authService, ApiResponseFactory responseFactory) {
        this.authService = authService;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return responseFactory.success(authService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return responseFactory.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.email(), request.password());
        return responseFactory.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request) {
        String accessToken = JwtService.extractBearerToken(authorization);
        String refreshToken = request == null ? null : request.refreshToken();
        authService.logout(accessToken, refreshToken);
        return responseFactory.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> me() {
        return responseFactory.success(authService.me());
    }
}
