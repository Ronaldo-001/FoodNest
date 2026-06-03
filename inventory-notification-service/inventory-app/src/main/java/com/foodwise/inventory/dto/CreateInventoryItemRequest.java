package com.foodwise.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateInventoryItemRequest {

    @NotNull(message = "Menu item ID is required")
    @Positive
    private Long menuItemId;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Quantity is required")
    @Min(0)
    private Integer quantity;

    @Size(max = 50)
    private String unit;

    @Min(value = 1, message = "Threshold must be at least 1")
    private Integer threshold;

    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;
}
