package com.foodwise.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMenuItemRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @Size(max = 100)
    private String category;

    @Pattern(regexp = "^(https?://.+)?$", message = "Image URL must be a valid URL or empty")
    @Size(max = 512)
    private String imageUrl;
}
