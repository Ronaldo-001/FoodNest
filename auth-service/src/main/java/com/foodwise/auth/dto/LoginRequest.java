package com.foodwise.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Size(max = 255)
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Size(max = 128)
    private String password;
}
