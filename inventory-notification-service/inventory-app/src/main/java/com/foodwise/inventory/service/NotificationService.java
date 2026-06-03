package com.foodwise.inventory.service;

import com.foodwise.inventory.dto.SubscribeRequest;
import com.foodwise.inventory.exception.InventoryException;
import com.foodwise.inventory.model.NotificationSubscription;
import com.foodwise.inventory.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSubscriptionRepository subscriptionRepository;

    @Transactional
    public NotificationSubscription subscribe(SubscribeRequest request, Long customerId, String customerEmail) {
        if (subscriptionRepository.existsByCustomerIdAndRestaurantId(customerId, request.getRestaurantId())) {
            throw new InventoryException("Already subscribed to this restaurant's alerts", HttpStatus.CONFLICT);
        }

        String email = request.getCustomerEmail() != null ? request.getCustomerEmail() : customerEmail;

        NotificationSubscription subscription = NotificationSubscription.builder()
                .customerId(customerId)
                .customerEmail(email)
                .restaurantId(request.getRestaurantId())
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<NotificationSubscription> getSubscriptions(Long customerId) {
        return subscriptionRepository.findByCustomerId(customerId);
    }
}
