package com.foodwise.inventory.client;

import com.foodwise.inventory.dto.ValidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Component
public class AuthValidationClient {

    private final WebClient webClient;
    private final StringRedisTemplate redis;
    private static final String CACHE_PREFIX = "inv:auth:validate:";

    public AuthValidationClient(
            WebClient.Builder webClientBuilder,
            StringRedisTemplate redis,
            @Value("${app.services.auth-url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
        this.redis = redis;
    }

    public ValidateResponse validate(String bearerToken) {
        try {
            return webClient.get()
                    .uri("/auth/validate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .bodyToMono(ValidateResponse.class)
                    .retry(1)
                    .block(Duration.ofSeconds(5));
        } catch (WebClientResponseException e) {
            log.warn("Auth validation failed: {}", e.getMessage());
            return ValidateResponse.builder().valid(false).build();
        } catch (Exception e) {
            log.error("Auth service unreachable: {}", e.getMessage());
            return ValidateResponse.builder().valid(false).build();
        }
    }
}
