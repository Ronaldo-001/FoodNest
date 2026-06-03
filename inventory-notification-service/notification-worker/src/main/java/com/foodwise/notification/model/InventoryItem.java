package com.foodwise.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/** Read-only view of inventory_items table — notification-worker only reads, never writes inventory */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    private Long id;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "menu_item_id")
    private Long menuItemId;

    private String name;
    private int quantity;
    private int threshold;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_surplus")
    private boolean surplus;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
