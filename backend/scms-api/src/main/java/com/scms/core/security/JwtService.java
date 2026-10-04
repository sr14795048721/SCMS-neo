package com.scms.core.security;

import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "typ";

    private final JwtProperties jwtProperties;
    private final TokenStore tokenStore;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties, TokenStore tokenStore) {
        this.jwtProperties = jwtProperties;
        this.tokenStore = tokenStore;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair issueTokenPair(UserEntity user) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(Duration.ofMinutes(jwtProperties.getAccessExpirationMinutes()));
        Instant refreshExpiresAt = now.plus(Duration.ofDays(jwtProperties.getRefreshExpirationDays()));

        String accessToken = buildToken(user, accessJti, TOKEN_TYPE_ACCESS, accessExpiresAt);
        String refreshToken = buildToken(user, refreshJti, TOKEN_TYPE_REFRESH, refreshExpiresAt);

        tokenStore.storeRefreshToken(refreshJti, user.getId(), refreshExpiresAt);

        return new TokenPair(
                accessToken,
                refreshToken,
                Duration.between(now, accessExpiresAt).toSeconds(),
                Duration.between(now, refreshExpiresAt).toSeconds()
        );
    }

    public JwtTokenClaims parseAccessToken(String token) {
        JwtTokenClaims claims = parseToken(token, TOKEN_TYPE_ACCESS);
        if (tokenStore.isAccessTokenBlacklisted(claims.tokenId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token revoked");
        }
        return claims;
    }

    public JwtTokenClaims parseRefreshToken(String token) {
        JwtTokenClaims claims = parseToken(token, TOKEN_TYPE_REFRESH);
        String userId = tokenStore.getRefreshTokenUserId(claims.tokenId());
        if (!Objects.equals(userId, String.valueOf(claims.userId()))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token revoked");
        }
        return claims;
    }

    public void revokeAccessToken(String token) {
        JwtTokenClaims claims = parseTokenWithoutBlacklist(token);
        tokenStore.blacklistAccessToken(claims.tokenId(), claims.expiresAt());
    }

    public void revokeRefreshToken(String token) {
        JwtTokenClaims claims = parseToken(token, TOKEN_TYPE_REFRESH);
        tokenStore.deleteRefreshToken(claims.tokenId());
    }

    public void revokeRefreshTokenByJti(String jti) {
        tokenStore.deleteRefreshToken(jti);
    }

    private String buildToken(UserEntity user, String jti, String tokenType, Instant expiresAt) {
        return Jwts.builder()
                .id(jti)
                .subject(user.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_UID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(secretKey)
                .compact();
    }

    private JwtTokenClaims parseTokenWithoutBlacklist(String token) {
        try {
            Jws<Claims> jwt = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return toClaims(jwt.getPayload());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token");
        }
    }

    private JwtTokenClaims parseToken(String token, String expectedType) {
        JwtTokenClaims claims = parseTokenWithoutBlacklist(token);
        if (!expectedType.equals(claims.tokenType())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token type");
        }
        return claims;
    }

    private JwtTokenClaims toClaims(Claims claims) {
        Object uid = claims.get(CLAIM_UID);
        Object role = claims.get(CLAIM_ROLE);
        Object tokenType = claims.get(CLAIM_TOKEN_TYPE);
        if (uid == null || role == null || tokenType == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token payload");
        }
        return new JwtTokenClaims(
                Long.valueOf(uid.toString()),
                claims.getSubject(),
                UserRole.valueOf(role.toString()),
                claims.getId(),
                tokenType.toString(),
                claims.getExpiration().toInstant()
        );
    }

    public String refreshTokenJti(JwtTokenClaims claims) {
        return claims.tokenId();
    }

    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    public void revokeRefreshByJtiWithFallback(String jti) {
        tokenStore.deleteRefreshToken(jti);
    }

    public boolean refreshTokenExists(String jti) {
        return tokenStore.refreshTokenExists(jti);
    }

    public void storeRefreshToken(String jti, Long userId, Instant expiresAt) {
        tokenStore.storeRefreshToken(jti, userId, expiresAt);
    }
}
