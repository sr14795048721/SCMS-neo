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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "typ";

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties, StringRedisTemplate redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
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

        redisTemplate.opsForValue().set(refreshKey(refreshJti), user.getId().toString(),
                Duration.between(now, refreshExpiresAt));

        return new TokenPair(
                accessToken,
                refreshToken,
                Duration.between(now, accessExpiresAt).toSeconds(),
                Duration.between(now, refreshExpiresAt).toSeconds()
        );
    }

    public JwtTokenClaims parseAccessToken(String token) {
        JwtTokenClaims claims = parseToken(token, TOKEN_TYPE_ACCESS);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(claims.tokenId())))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token revoked");
        }
        return claims;
    }

    public JwtTokenClaims parseRefreshToken(String token) {
        JwtTokenClaims claims = parseToken(token, TOKEN_TYPE_REFRESH);
        String userId = redisTemplate.opsForValue().get(refreshKey(claims.tokenId()));
        if (!Objects.equals(userId, String.valueOf(claims.userId()))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token revoked");
        }
        return claims;
    }

    public void revokeAccessToken(String token) {
        JwtTokenClaims claims = parseTokenWithoutBlacklist(token);
        Duration ttl = Duration.between(Instant.now(), claims.expiresAt());
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(blacklistKey(claims.tokenId()), "1", ttl);
        }
    }

    public void revokeRefreshToken(String token) {
        JwtTokenClaims claims = parseToken(token, TOKEN_TYPE_REFRESH);
        redisTemplate.delete(refreshKey(claims.tokenId()));
    }

    public void revokeRefreshTokenByJti(String jti) {
        redisTemplate.delete(refreshKey(jti));
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

    private String blacklistKey(String jti) {
        return "auth:blacklist:" + jti;
    }

    private String refreshKey(String jti) {
        return "auth:refresh:" + jti;
    }

    public void revokeRefreshByJtiWithFallback(String jti) {
        redisTemplate.delete(refreshKey(jti));
    }

    public boolean refreshTokenExists(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey(jti)));
    }

    public void storeRefreshToken(String jti, Long userId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(refreshKey(jti), String.valueOf(userId), ttl.toSeconds(), TimeUnit.SECONDS);
        }
    }
}
