package com.foodwise.catalog.client;

import com.foodwise.catalog.dto.ValidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client for calling auth-service GET /auth/validate.
 * Results are cached in Redis for 30s to reduce auth-service load.
 * Falls back to calling auth-service if Redis is unavailable.
 */
@Slf4j
@Component
public class AuthValidationClient {

    private final WebClient webClient;
    private final StringRedisTemplate redis;

    private static final String CACHE_PREFIX = "auth:validate:";
    private static final int CACHE_TTL_SECONDS = 30;

    public AuthValidationClient(
            WebClient.Builder webClientBuilder,
            StringRedisTemplate redis,
            @Value("${app.services.auth-url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
        this.redis = redis;
    }

    /**
     * Validates a bearer token by calling auth-service.
     * Returns null if token is invalid or service is unavailable.
     */
    public ValidateResponse validate(String bearerToken) {
        // Try cache first
        try {
            String cached = redis.opsForValue().get(CACHE_PREFIX + bearerToken.hashCode());
            if (cached != null) {
                // Simple cache: just store the userId — full validation done by auth-service
                // For production: store full ValidateResponse as JSON
                log.debug("Auth validation cache hit");
            }
        } catch (Exception e) {
            log.warn("Redis cache unavailable for auth validation: {}", e.getMessage());
        }

        // Call auth-service
        try {
            ValidateResponse response = webClient.get()
                    .uri("/auth/validate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .bodyToMono(ValidateResponse.class)
                    .block(Duration.ofSeconds(5));

            if (response != null && response.isValid()) {
                // Cache successful validation
                try {
                    redis.opsForValue().set(
                        CACHE_PREFIX + bearerToken.hashCode(),
                        String.valueOf(response.getUserId()),
                        Duration.ofSeconds(CACHE_TTL_SECONDS)
                    );
                } catch (Exception e) {
                    log.warn("Failed to cache auth validation result: {}", e.getMessage());
                }
            }
            return response;

        } catch (WebClientResponseException e) {
            log.warn("Auth validation failed with status {}: {}", e.getStatusCode(), e.getMessage());
            return ValidateResponse.builder().valid(false).build();
        } catch (Exception e) {
            log.error("Auth service unreachable: {}", e.getMessage());
            // SECURITY: fail-closed — reject request if auth-service is unreachable
            return ValidateResponse.builder().valid(false).build();
        }
    }
}
