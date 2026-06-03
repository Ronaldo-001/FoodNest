package com.foodwise.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SubscribeRequest {

    @NotNull
    @Positive
    private Long restaurantId;

    @Email
    private String customerEmail;  // Optional: if not provided, uses email from JWT
}
