package com.foodwise.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DeductRequest {

    @NotNull
    @Positive
    private Long menuItemId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
