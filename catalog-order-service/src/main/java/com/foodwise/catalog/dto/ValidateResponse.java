package com.foodwise.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Mirrors auth-service ValidateResponse — used by AuthValidationClient.
 * Kept separate to avoid cross-module dependencies.
 */
@Data
@Builder
public class ValidateResponse {
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
    private Long restaurantId;
    private boolean valid;
}
