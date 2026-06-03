package com.foodwise.catalog.dto;

import com.foodwise.catalog.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateDto {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
