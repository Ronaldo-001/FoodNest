package com.foodwise.auth.controller;

import com.foodwise.auth.dto.*;
import com.foodwise.auth.service.AuthService;
import com.foodwise.auth.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth REST controller — no business logic lives here.
 * All logic delegated to AuthService.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        rateLimitService.checkRegisterRateLimit(getClientIp(httpRequest));
        TokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        rateLimitService.checkLoginRateLimit(getClientIp(httpRequest));
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            Authentication authentication) {
        String token = extractBearerToken(httpRequest);
        Long userId = (Long) authentication.getPrincipal();
        authService.logout(token, userId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Internal endpoint — called by other microservices to validate JWTs.
     * Requires the calling service to pass a valid JWT.
     */
    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(HttpServletRequest httpRequest) {
        String token = extractBearerToken(httpRequest);
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.ok(ValidateResponse.builder().valid(false).build());
        }
        ValidateResponse response = authService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For when behind a proxy/load balancer
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // Take first IP in the chain (client IP)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
