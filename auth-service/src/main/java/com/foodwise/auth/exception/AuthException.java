package com.foodwise.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // ─── Factory methods ─────────────────────────────────────────────────────

    public static AuthException invalidCredentials() {
        // SECURITY: generic message — do not leak whether username or password was wrong
        return new AuthException("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    public static AuthException userAlreadyExists(String field) {
        return new AuthException(field + " is already taken", HttpStatus.CONFLICT);
    }

    public static AuthException tokenExpired() {
        return new AuthException("Token has expired", HttpStatus.UNAUTHORIZED);
    }

    public static AuthException tokenInvalid() {
        return new AuthException("Token is invalid", HttpStatus.UNAUTHORIZED);
    }

    public static AuthException roleNotFound(String role) {
        return new AuthException("Role not found: " + role, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static AuthException accountDisabled() {
        return new AuthException("Account is disabled", HttpStatus.FORBIDDEN);
    }
}
