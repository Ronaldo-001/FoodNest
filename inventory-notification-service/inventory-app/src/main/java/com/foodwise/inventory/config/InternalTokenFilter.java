package com.foodwise.inventory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guards internal-only endpoints (/inventory/deduct and /inventory/restore).
 * Checks the Authorization header for the shared INTERNAL_SERVICE_TOKEN.
 *
 * SECURITY: These endpoints must NOT be accessible from outside the service network.
 * In production (K8s + Istio), mTLS at the mesh level replaces this token.
 * TODO(security): Replace with mTLS when deploying to K8s with Istio.
 */
@Slf4j
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${app.services.internal-token}")
    private String internalToken;

    private static final String[] INTERNAL_PATHS = {"/inventory/deduct", "/inventory/restore"};

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isInternalPath = false;
        for (String internalPath : INTERNAL_PATHS) {
            if (path.equals(internalPath)) {
                isInternalPath = true;
                break;
            }
        }

        if (isInternalPath) {
            String token = extractBearerToken(request);
            if (!StringUtils.hasText(token) || !token.equals(internalToken)) {
                log.warn("Unauthorized internal endpoint access attempt to {}", path);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("{\"error\":\"Unauthorized\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
