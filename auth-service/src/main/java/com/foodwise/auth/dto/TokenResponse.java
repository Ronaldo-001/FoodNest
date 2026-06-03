package com.foodwise.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String refreshToken;
    private Long userId;
    private String username;
    private List<String> roles;
}
