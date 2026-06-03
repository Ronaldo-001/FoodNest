package com.foodwise.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSubscription {

    @Id
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
