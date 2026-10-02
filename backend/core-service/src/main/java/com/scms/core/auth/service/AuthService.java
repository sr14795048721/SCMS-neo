package com.scms.core.auth.service;

import com.scms.core.auth.dto.TokenResponse;
import com.scms.core.auth.dto.UserMeResponse;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.security.JwtService;
import com.scms.core.security.JwtTokenClaims;
import com.scms.core.security.TokenPair;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserProvider currentUserProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String usernameOrEmail, String password) {
        String identifier = sanitize(usernameOrEmail);
        String emailCandidate = normalizeEmail(identifier);

        UserEntity user = userRepository.findByUsernameOrEmail(identifier, emailCandidate)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        TokenPair pair = jwtService.issueTokenPair(user);
        return toTokenResponse(pair);
    }

    @Transactional
    public void register(String username, String email, String password) {
        String normalizedUsername = sanitize(username);
        String normalizedEmail = normalizeEmail(sanitize(email));

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.STUDENT);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        JwtTokenClaims claims = jwtService.parseRefreshToken(refreshToken);
        UserEntity user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "user not found"));
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "user is disabled");
        }
        jwtService.revokeRefreshTokenByJti(jwtService.refreshTokenJti(claims));
        TokenPair pair = jwtService.issueTokenPair(user);
        return toTokenResponse(pair);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            try {
                jwtService.revokeAccessToken(accessToken);
            } catch (RuntimeException ignored) {
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                jwtService.revokeRefreshToken(refreshToken);
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Transactional(readOnly = true)
    public UserMeResponse me() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        return new UserMeResponse(user.userId(), user.username(), user.role().name());
    }

    private TokenResponse toTokenResponse(TokenPair pair) {
        return new TokenResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessExpiresInSeconds(),
                pair.refreshExpiresInSeconds()
        );
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
