package com.foodwise.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response from GET /auth/validate — consumed by other microservices.
 * Contains enough identity info to build a SecurityContext in the calling service.
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
