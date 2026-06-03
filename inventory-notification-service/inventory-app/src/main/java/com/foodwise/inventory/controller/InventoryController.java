package com.foodwise.inventory.controller;

import com.foodwise.inventory.dto.*;
import com.foodwise.inventory.model.NotificationSubscription;
import com.foodwise.inventory.service.InventoryService;
import com.foodwise.inventory.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    @GetMapping("/inventory/items")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<InventoryItemDto>> getInventory(Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(inventoryService.getRestaurantInventory(caller.getRestaurantId()));
    }

    @PostMapping("/inventory/items")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<InventoryItemDto> createItem(
            @Valid @RequestBody CreateInventoryItemRequest request,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.create(request, caller.getRestaurantId()));
    }

    @PutMapping("/inventory/items/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<InventoryItemDto> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateInventoryItemRequest request,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(inventoryService.update(id, request, caller.getRestaurantId()));
    }

    @GetMapping("/inventory/surplus")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<InventoryItemDto>> getSurplusItems(Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(inventoryService.getSurplusItems(caller.getRestaurantId()));
    }

    /** Internal endpoint — protected by InternalTokenFilter (INTERNAL_SERVICE_TOKEN) */
    @PostMapping("/inventory/deduct")
    public ResponseEntity<Void> deductStock(
            @Valid @RequestBody DeductRequest request,
            @RequestParam(required = false) Long restaurantId) {
        // For internal calls, restaurantId can be provided as query param
        // or resolved from the inventory item directly
        inventoryService.deductStock(request, restaurantId != null ? restaurantId : 0L);
        return ResponseEntity.ok().build();
    }

    /** Internal endpoint — protected by InternalTokenFilter */
    @PostMapping("/inventory/restore")
    public ResponseEntity<Void> restoreStock(
            @Valid @RequestBody DeductRequest request,
            @RequestParam(required = false) Long restaurantId) {
        inventoryService.restoreStock(request, restaurantId != null ? restaurantId : 0L);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/subscribe")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        notificationService.subscribe(request, caller.getUserId(), caller.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Successfully subscribed to surplus alerts"));
    }

    @GetMapping("/notifications/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<NotificationSubscription>> getSubscriptions(Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getSubscriptions(caller.getUserId()));
    }
}
