package com.foodwise.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
