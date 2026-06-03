package com.foodwise.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotNull(message = "Restaurant ID is required")
    @Positive
    private Long restaurantId;

    @Size(max = 1000)
    private String notes;

    @NotEmpty(message = "Order must have at least one item")
    @Size(max = 50, message = "Order cannot exceed 50 items")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {

        @NotNull(message = "Menu item ID is required")
        @Positive
        private Long menuItemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100")
        private Integer quantity;
    }
}
