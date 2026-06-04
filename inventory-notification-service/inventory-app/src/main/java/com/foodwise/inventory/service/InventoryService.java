package com.foodwise.inventory.service;

import com.foodwise.inventory.dto.CreateInventoryItemRequest;
import com.foodwise.inventory.dto.DeductRequest;
import com.foodwise.inventory.dto.InventoryItemDto;
import com.foodwise.inventory.exception.InventoryException;
import com.foodwise.inventory.model.InventoryItem;
import com.foodwise.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final SurplusDetectionService surplusDetectionService;

    @Value("${app.surplus.expiry-hours:24}")
    private int surplusExpiryHours;

    @Transactional(readOnly = true)
    public List<InventoryItemDto> getRestaurantInventory(Long restaurantId) {
        return inventoryItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> getSurplusItems(Long restaurantId) {
        LocalDate expiryThreshold = LocalDate.now().plusDays(surplusExpiryHours / 24);
        return inventoryItemRepository.findSurplusItems(restaurantId, expiryThreshold).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryItemDto create(CreateInventoryItemRequest request, Long restaurantId) {
        // Check for duplicate
        inventoryItemRepository.findByRestaurantIdAndMenuItemId(restaurantId, request.getMenuItemId())
                .ifPresent(existing -> {
                    throw new InventoryException("Inventory item already exists for this menu item", HttpStatus.CONFLICT);
                });

        InventoryItem item = InventoryItem.builder()
                .restaurantId(restaurantId)
                .menuItemId(request.getMenuItemId())
                .name(request.getName())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .threshold(request.getThreshold() != null ? request.getThreshold() : 10)
                .expiryDate(request.getExpiryDate())
                .build();

        InventoryItem saved = inventoryItemRepository.save(item);
        surplusDetectionService.checkAndMarkSurplus(saved);
        return toDto(saved);
    }

    @Transactional
    public InventoryItemDto update(Long itemId, CreateInventoryItemRequest request, Long restaurantId) {
        InventoryItem item = inventoryItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new InventoryException("Inventory item not found", HttpStatus.NOT_FOUND));

        item.setQuantity(request.getQuantity());
        item.setName(request.getName());
        if (request.getUnit() != null) item.setUnit(request.getUnit());
        if (request.getThreshold() != null) item.setThreshold(request.getThreshold());
        if (request.getExpiryDate() != null) item.setExpiryDate(request.getExpiryDate());

        InventoryItem saved = inventoryItemRepository.save(item);
        surplusDetectionService.checkAndMarkSurplus(saved);
        return toDto(saved);
    }

    /**
     * Internal endpoint — called by catalog-order-service when an order is placed.
     * SECURITY: protected by INTERNAL_SERVICE_TOKEN check in SecurityConfig.
     */
    @Transactional
    public void deductStock(DeductRequest request, Long restaurantId) {
        // restaurantId is resolved from the item's restaurantId — internal calls pass item's restaurantId
        InventoryItem item = inventoryItemRepository.findByRestaurantIdAndMenuItemId(
                restaurantId, request.getMenuItemId())
                .orElse(null);

        if (item == null) {
            log.warn("Inventory item not found for deduction: menuItemId={}", request.getMenuItemId());
            return;
        }

        int newQty = item.getQuantity() - request.getQuantity();
        if (newQty < 0) {
            log.warn("Stock deduction would result in negative quantity for item {}. Clamping to 0.", item.getId());
            newQty = 0;
        }

        item.setQuantity(newQty);
        InventoryItem saved = inventoryItemRepository.save(item);
        surplusDetectionService.checkAndMarkSurplus(saved);
    }

    /**
     * Internal endpoint — called by catalog-order-service when an order is cancelled.
     */
    @Transactional
    public void restoreStock(DeductRequest request, Long restaurantId) {
        InventoryItem item = inventoryItemRepository.findByRestaurantIdAndMenuItemId(
                restaurantId, request.getMenuItemId())
                .orElse(null);

        if (item == null) {
            log.warn("Inventory item not found for restore: menuItemId={}", request.getMenuItemId());
            return;
        }

        item.setQuantity(item.getQuantity() + request.getQuantity());
        inventoryItemRepository.save(item);
    }

    private InventoryItemDto toDto(InventoryItem item) {
        return InventoryItemDto.builder()
                .id(item.getId())
                .restaurantId(item.getRestaurantId())
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .threshold(item.getThreshold())
                .expiryDate(item.getExpiryDate())
                .surplus(item.isSurplus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
