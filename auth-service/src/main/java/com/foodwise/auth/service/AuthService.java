package com.foodwise.auth.service;

import com.foodwise.auth.dto.*;
import com.foodwise.auth.exception.AuthException;
import com.foodwise.auth.model.RefreshToken;
import com.foodwise.auth.model.Role;
import com.foodwise.auth.model.User;
import com.foodwise.auth.repository.RoleRepository;
import com.foodwise.auth.repository.UserRepository;
import com.foodwise.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // SECURITY: check existence before hashing (avoid unnecessary BCrypt work on duplicate)
        if (userRepository.existsByUsername(request.getUsername())) {
            throw AuthException.userAlreadyExists("Username");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AuthException.userAlreadyExists("Email");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> AuthException.roleNotFound(request.getRole()));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // SECURITY: BCrypt hash — never store plaintext
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .restaurantId(request.getRestaurantId())
                .roles(Set.of(role))
                .build();

        user = userRepository.save(user);
        log.info("New user registered: id={}, role={}", user.getId(), request.getRole());

        return buildTokenResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                // SECURITY: generic error — do not reveal whether user exists
                .orElseThrow(AuthException::invalidCredentials);

        if (!user.isActive()) {
            throw AuthException.accountDisabled();
        }

        // SECURITY: constant-time comparison via BCrypt matches()
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // SECURITY: same error message as "user not found"
            throw AuthException.invalidCredentials();
        }

        log.info("User logged in: id={}", user.getId());
        return buildTokenResponse(user);
    }

    @Transactional
    public void logout(String accessToken, Long userId) {
        // Revoke all refresh tokens for this user
        tokenService.revokeAllUserTokens(userId);

        // Blacklist the current access token until it naturally expires
        try {
            Claims claims = jwtTokenProvider.validateAndParseClaims(accessToken);
            String jti = claims.getId();
            long remainingMs = claims.getExpiration().getTime() - Instant.now().toEpochMilli();
            tokenService.blacklistAccessToken(jti, remainingMs);
        } catch (JwtException e) {
            log.warn("Could not parse access token during logout: {}", e.getMessage());
        }
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken newRefreshToken = tokenService.validateAndRotate(request.getRefreshToken());
        User user = newRefreshToken.getUser();
        return buildTokenResponse(user);
    }

    public ValidateResponse validateToken(String token) {
        if (!jwtTokenProvider.isTokenValid(token)) {
            return ValidateResponse.builder().valid(false).build();
        }

        try {
            Claims claims = jwtTokenProvider.validateAndParseClaims(token);
            String jti = claims.getId();

            // Check blacklist (Redis fail-closed)
            if (jti != null && tokenService.isAccessTokenBlacklisted(jti)) {
                return ValidateResponse.builder().valid(false).build();
            }

            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            // Fetch from DB to get up-to-date restaurantId and active status
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                return ValidateResponse.builder().valid(false).build();
            }

            return ValidateResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .username(username)
                    .email(user.getEmail())
                    .roles(roles)
                    .restaurantId(user.getRestaurantId())
                    .build();

        } catch (Exception e) {
            log.warn("Token validation error: {}", e.getMessage());
            return ValidateResponse.builder().valid(false).build();
        }
    }

    private TokenResponse buildTokenResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
        RefreshToken refreshToken = tokenService.createRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(900)   // 15 minutes in seconds
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roles)
                .build();
    }
}
