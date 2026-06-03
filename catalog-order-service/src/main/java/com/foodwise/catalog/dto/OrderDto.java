package com.foodwise.catalog.dto;

import com.foodwise.catalog.model.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderDto {
    private Long id;
    private Long customerId;
    private Long restaurantId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String notes;
    private List<OrderItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    public static class OrderItemDto {
        private Long id;
        private Long menuItemId;
        private String menuItemName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
