package com.scms.core.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 TokenStore 实现：ConcurrentHashMap + 惰性过期。
 * 单实例部署语义与 Redis 一致；重启后 refresh token 失效，用户需重新登录。
 */
@Component
public class InMemoryTokenStore implements TokenStore {

    private final Map<String, Entry> refreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Entry> blacklist = new ConcurrentHashMap<>();

    @Override
    public void storeRefreshToken(String jti, Long userId, Instant expiresAt) {
        if (!expiresAt.isAfter(Instant.now())) {
            return;
        }
        refreshTokens.put(jti, Entry.of(String.valueOf(userId), expiresAt));
    }

    @Override
    public String getRefreshTokenUserId(String jti) {
        Entry entry = refreshTokens.get(jti);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            refreshTokens.remove(jti);
            return null;
        }
        return entry.value();
    }

    @Override
    public void deleteRefreshToken(String jti) {
        refreshTokens.remove(jti);
    }

    @Override
    public boolean refreshTokenExists(String jti) {
        return getRefreshTokenUserId(jti) != null;
    }

    @Override
    public void blacklistAccessToken(String jti, Instant expiresAt) {
        if (!expiresAt.isAfter(Instant.now())) {
            return;
        }
        blacklist.put(jti, Entry.of("1", expiresAt));
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        Entry entry = blacklist.get(jti);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    private record Entry(String value, Instant expiresAt) {

        private static Entry of(String value, Instant expiresAt) {
            return new Entry(value, expiresAt);
        }

        private boolean isExpired() {
            return !expiresAt.isAfter(Instant.now());
        }
    }
}
