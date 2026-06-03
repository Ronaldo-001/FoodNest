package com.foodwise.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username may only contain letters, digits, underscores, hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    /**
     * Password policy:
     * - 8-128 characters
     * - No character type requirements (users choose strong passwords)
     * - All characters allowed
     * TODO(security): Integrate leaked password detection (e.g., HIBP API)
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(CUSTOMER|RESTAURANT_OWNER)$", message = "Role must be CUSTOMER or RESTAURANT_OWNER")
    private String role;

    /** Optional: only for RESTAURANT_OWNER role */
    private Long restaurantId;
}
