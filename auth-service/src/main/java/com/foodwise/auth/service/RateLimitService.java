package com.foodwise.auth.service;

import com.foodwise.auth.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed sliding window rate limiter.
 * Falls back to allow-through (fail-open) if Redis is unavailable,
 * logging a warning. For strict fail-closed behavior, flip the catch block.
 *
 * SECURITY: Rate limits login and register endpoints to prevent brute force.
 */
@Slf4j
@Service
public class RateLimitService {

    private final StringRedisTemplate redis;

    @Value("${app.rate-limit.login.max-attempts}")
    private int loginMaxAttempts;

    @Value("${app.rate-limit.login.window-seconds}")
    private int loginWindowSeconds;

    @Value("${app.rate-limit.register.max-attempts}")
    private int registerMaxAttempts;

    @Value("${app.rate-limit.register.window-seconds}")
    private int registerWindowSeconds;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void checkLoginRateLimit(String clientIp) {
        checkRateLimit("rl:login:" + clientIp, loginMaxAttempts, loginWindowSeconds,
                "Too many login attempts. Please try again in " + loginWindowSeconds + " seconds.");
    }

    public void checkRegisterRateLimit(String clientIp) {
        checkRateLimit("rl:register:" + clientIp, registerMaxAttempts, registerWindowSeconds,
                "Too many registration attempts. Please try again later.");
    }

    private void checkRateLimit(String key, int maxAttempts, int windowSeconds, String errorMessage) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                // First attempt in window — set expiry
                redis.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > maxAttempts) {
                throw new RateLimitException(errorMessage);
            }
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            // Redis unavailable — fail-open with warning
            // TODO(security): Consider fail-closed for stricter security
            log.warn("Rate limit Redis unavailable for key {}: {} — allowing request", key, e.getMessage());
        }
    }
}
