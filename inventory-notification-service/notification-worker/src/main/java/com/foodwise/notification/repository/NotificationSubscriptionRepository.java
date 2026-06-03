package com.foodwise.notification.repository;

import com.foodwise.notification.model.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {
    List<NotificationSubscription> findByRestaurantId(Long restaurantId);
}
