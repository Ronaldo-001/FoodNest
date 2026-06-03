package com.foodwise.inventory.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 0;

    @Column(length = 50)
    private String unit;

    @Column(nullable = false)
    @Builder.Default
    private int threshold = 10;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_surplus", nullable = false)
    @Builder.Default
    private boolean surplus = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
