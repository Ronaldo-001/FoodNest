package com.foodwise.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class InventoryItemDto {
    private Long id;
    private Long restaurantId;
    private Long menuItemId;
    private String name;
    private int quantity;
    private String unit;
    private int threshold;
    private LocalDate expiryDate;
    private boolean surplus;
    private Instant createdAt;
    private Instant updatedAt;
}
