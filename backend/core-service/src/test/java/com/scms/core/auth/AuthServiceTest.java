package com.scms.core.auth;

import com.scms.core.auth.dto.TokenResponse;
import com.scms.core.auth.service.AuthService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.security.JwtService;
import com.scms.core.security.TokenPair;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, mock(CurrentUserProvider.class));
    }

    @Test
    void loginShouldReturnTokensWhenCredentialsAreValid() {
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);

        when(userRepository.findByUsernameOrEmail("admin", "admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "encoded")).thenReturn(true);
        when(jwtService.issueTokenPair(user)).thenReturn(new TokenPair("a", "r", 1800, 604800));

        TokenResponse response = authService.login("admin", "admin123");
        assertEquals("a", response.accessToken());
        assertEquals("r", response.refreshToken());
    }

    @Test
    void loginShouldThrowWhenPasswordMismatch() {
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login("admin", "wrong"));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }
}
