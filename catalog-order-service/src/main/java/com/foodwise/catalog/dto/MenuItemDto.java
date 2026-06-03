package com.foodwise.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class MenuItemDto {
    private Long id;
    private Long restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private boolean available;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
