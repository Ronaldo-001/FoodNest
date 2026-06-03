package com.foodwise.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "surplus_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurplusAlert {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    private String reason;

    @CreationTimestamp
    @Column(name = "triggered_at")
    private Instant triggeredAt;

    private boolean notified;

    @Column(name = "notified_at")
    private Instant notifiedAt;
}
