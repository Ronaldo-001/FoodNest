package com.foodwise.inventory.repository;

import com.foodwise.inventory.model.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

    List<NotificationSubscription> findByRestaurantId(Long restaurantId);

    boolean existsByCustomerIdAndRestaurantId(Long customerId, Long restaurantId);

    List<NotificationSubscription> findByCustomerId(Long customerId);
}
