package com.foodwise.auth.service;

import com.foodwise.auth.exception.AuthException;
import com.foodwise.auth.model.RefreshToken;
import com.foodwise.auth.model.User;
import com.foodwise.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Manages refresh token lifecycle and access token blacklisting via Redis.
 *
 * Token blacklist: When a user logs out, the access token JTI (jti claim)
 * is stored in Redis with TTL equal to remaining token lifetime.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redis;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken validateAndRotate(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .orElseThrow(AuthException::tokenInvalid);

        if (existing.isExpired()) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw AuthException.tokenExpired();
        }

        // Token rotation: revoke old, issue new
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return createRefreshToken(existing.getUser());
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /** Blacklist an access token by its JTI until it would naturally expire */
    public void blacklistAccessToken(String jti, long remainingTtlMs) {
        if (jti == null || jti.isBlank() || remainingTtlMs <= 0) return;
        try {
            redis.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "1",
                Duration.ofMillis(remainingTtlMs)
            );
        } catch (Exception e) {
            log.warn("Failed to blacklist token JTI {} in Redis: {}", jti, e.getMessage());
        }
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            log.warn("Redis check for blacklist failed: {} — failing closed", e.getMessage());
            // Fail-closed: if Redis is unavailable, treat token as blacklisted
            return true;
        }
    }

    /** Scheduled cleanup of expired and revoked refresh tokens */
    @Scheduled(cron = "0 0 2 * * *") // 02:00 daily
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        log.info("Cleaned up expired/revoked refresh tokens");
    }
}
