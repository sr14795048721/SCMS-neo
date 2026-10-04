package com.scms.core.security;

import java.time.Instant;

/**
 * Token 状态存储抽象：refresh token 登记与 access token 黑名单。
 */
public interface TokenStore {

    /** 登记一个 refresh token（jti → userId），到期后自动失效。 */
    void storeRefreshToken(String jti, Long userId, Instant expiresAt);

    /** 读取 refresh token 对应的 userId，不存在或已过期返回 null。 */
    String getRefreshTokenUserId(String jti);

    void deleteRefreshToken(String jti);

    boolean refreshTokenExists(String jti);

    /** 将 access token jti 加入黑名单，expiresAt 后自动解除（token 已自然过期）。 */
    void blacklistAccessToken(String jti, Instant expiresAt);

    boolean isAccessTokenBlacklisted(String jti);
}
