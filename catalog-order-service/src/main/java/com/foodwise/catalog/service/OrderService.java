package com.foodwise.catalog.service;

import com.foodwise.catalog.client.InventoryClient;
import com.foodwise.catalog.dto.CreateOrderRequest;
import com.foodwise.catalog.dto.OrderDto;
import com.foodwise.catalog.dto.OrderStatusUpdateDto;
import com.foodwise.catalog.dto.ValidateResponse;
import com.foodwise.catalog.exception.CatalogException;
import com.foodwise.catalog.model.MenuItem;
import com.foodwise.catalog.model.Order;
import com.foodwise.catalog.model.OrderItem;
import com.foodwise.catalog.model.OrderStatus;
import com.foodwise.catalog.repository.MenuItemRepository;
import com.foodwise.catalog.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryClient inventoryClient;

    // Valid state transitions
    private static final java.util.Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = java.util.Map.of(
        OrderStatus.PENDING,    Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED,  Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        OrderStatus.PREPARING,  Set.of(OrderStatus.READY),
        OrderStatus.READY,      Set.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED,  Set.of(),
        OrderStatus.CANCELLED,  Set.of()
    );

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, ValidateResponse caller) {
        if (!caller.getRoles().contains("CUSTOMER")) {
            throw new CatalogException("Only customers can place orders", HttpStatus.FORBIDDEN);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        Order order = Order.builder()
                .customerId(caller.getUserId())
                .restaurantId(request.getRestaurantId())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .build();

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new CatalogException(
                        "Menu item not found: " + itemReq.getMenuItemId(), HttpStatus.NOT_FOUND));

            // SECURITY: verify menu item belongs to requested restaurant
            if (!menuItem.getRestaurantId().equals(request.getRestaurantId())) {
                throw new CatalogException(
                    "Menu item does not belong to the specified restaurant", HttpStatus.BAD_REQUEST);
            }

            if (!menuItem.isAvailable()) {
                throw new CatalogException("Menu item is not available: " + menuItem.getName(), HttpStatus.CONFLICT);
            }

            BigDecimal subtotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItemId(menuItem.getId())
                    .menuItemName(menuItem.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .subtotal(subtotal)
                    .build();
            orderItems.add(orderItem);
        }

        order.setTotalAmount(total);
        order.setItems(orderItems);
        Order saved = orderRepository.save(order);

        // Deduct inventory for each item (best-effort, logs warning on failure)
        for (OrderItem item : orderItems) {
            boolean deducted = inventoryClient.deductStock(item.getMenuItemId(), item.getQuantity());
            if (!deducted) {
                log.warn("Inventory deduction failed for order {} item {}", saved.getId(), item.getMenuItemId());
            }
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto getById(Long orderId, ValidateResponse caller) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CatalogException("Order not found", HttpStatus.NOT_FOUND));

        // SECURITY: only the customer who placed the order or the restaurant owner can view it
        boolean isOwner = caller.getRoles().contains("RESTAURANT_OWNER") &&
                          caller.getRestaurantId() != null &&
                          caller.getRestaurantId().equals(order.getRestaurantId());
        boolean isCustomer = caller.getUserId().equals(order.getCustomerId());

        if (!isOwner && !isCustomer && !caller.getRoles().contains("ADMIN")) {
            throw new CatalogException("Access denied", HttpStatus.FORBIDDEN);
        }

        return toDto(order);
    }

    @Transactional
    public OrderDto updateStatus(Long orderId, OrderStatusUpdateDto statusUpdate, ValidateResponse caller) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CatalogException("Order not found", HttpStatus.NOT_FOUND));

        // SECURITY: only restaurant owner or admin can update status
        if (!caller.getRoles().contains("RESTAURANT_OWNER") && !caller.getRoles().contains("ADMIN")) {
            throw new CatalogException("Access denied", HttpStatus.FORBIDDEN);
        }

        OrderStatus newStatus = statusUpdate.getStatus();
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new CatalogException(
                "Invalid status transition from " + order.getStatus() + " to " + newStatus,
                HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Restore inventory if cancelling
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                inventoryClient.restoreStock(item.getMenuItemId(), item.getQuantity());
            }
        }

        order.setStatus(newStatus);
        return toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByRestaurant(Long restaurantId, ValidateResponse caller, Pageable pageable) {
        // SECURITY: owner can only see their own restaurant's orders
        if (caller.getRoles().contains("RESTAURANT_OWNER") &&
            !caller.getRestaurantId().equals(restaurantId)) {
            throw new CatalogException("Access denied", HttpStatus.FORBIDDEN);
        }
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByCustomer(Long customerId, ValidateResponse caller, Pageable pageable) {
        // SECURITY: customers can only see their own orders
        if (caller.getRoles().contains("CUSTOMER") && !caller.getUserId().equals(customerId)) {
            throw new CatalogException("Access denied", HttpStatus.FORBIDDEN);
        }
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable).map(this::toDto);
    }

    private OrderDto toDto(Order order) {
        List<OrderDto.OrderItemDto> items = order.getItems().stream()
                .map(item -> OrderDto.OrderItemDto.builder()
                        .id(item.getId())
                        .menuItemId(item.getMenuItemId())
                        .menuItemName(item.getMenuItemName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return OrderDto.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .restaurantId(order.getRestaurantId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
