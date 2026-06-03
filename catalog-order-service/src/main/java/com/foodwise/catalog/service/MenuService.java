package com.foodwise.catalog.service;

import com.foodwise.catalog.dto.CreateMenuItemRequest;
import com.foodwise.catalog.dto.MenuItemDto;
import com.foodwise.catalog.dto.ValidateResponse;
import com.foodwise.catalog.exception.CatalogException;
import com.foodwise.catalog.model.MenuItem;
import com.foodwise.catalog.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final StringRedisTemplate redis;

    @Value("${app.cache.menu-item-ttl-seconds:300}")
    private int menuCacheTtl;

    private static final String MENU_CACHE_PREFIX = "menu:restaurant:";

    /** Public — returns all available items across all restaurants (paginated) */
    @Transactional(readOnly = true)
    public Page<MenuItemDto> getAllAvailableItems(Pageable pageable) {
        return menuItemRepository.findByAvailableTrue(pageable).map(this::toDto);
    }

    /** Public — returns available items for a specific restaurant */
    @Transactional(readOnly = true)
    public Page<MenuItemDto> getRestaurantMenu(Long restaurantId, Pageable pageable) {
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public MenuItemDto getById(Long id) {
        return menuItemRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new CatalogException("Menu item not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public MenuItemDto create(CreateMenuItemRequest request, ValidateResponse caller) {
        // SECURITY: restaurant owner can only create items for their own restaurant
        if (!caller.getRoles().contains("RESTAURANT_OWNER")) {
            throw new CatalogException("Access denied", HttpStatus.FORBIDDEN);
        }
        if (!caller.getRestaurantId().equals(request.getRestaurantId() != null
                ? request.getRestaurantId() : caller.getRestaurantId())) {
            throw new CatalogException("Cannot create items for another restaurant", HttpStatus.FORBIDDEN);
        }

        MenuItem item = MenuItem.builder()
                .restaurantId(caller.getRestaurantId())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .available(true)
                .build();

        MenuItem saved = menuItemRepository.save(item);
        invalidateMenuCache(caller.getRestaurantId());
        return toDto(saved);
    }

    @Transactional
    public MenuItemDto update(Long itemId, CreateMenuItemRequest request, ValidateResponse caller) {
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, caller.getRestaurantId())
                .orElseThrow(() -> new CatalogException("Menu item not found or access denied", HttpStatus.NOT_FOUND));

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl());
        }

        MenuItem saved = menuItemRepository.save(item);
        invalidateMenuCache(caller.getRestaurantId());
        return toDto(saved);
    }

    @Transactional
    public void delete(Long itemId, ValidateResponse caller) {
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, caller.getRestaurantId())
                .orElseThrow(() -> new CatalogException("Menu item not found or access denied", HttpStatus.NOT_FOUND));

        menuItemRepository.delete(item);
        invalidateMenuCache(caller.getRestaurantId());
    }

    private void invalidateMenuCache(Long restaurantId) {
        try {
            redis.delete(MENU_CACHE_PREFIX + restaurantId);
        } catch (Exception e) {
            log.warn("Failed to invalidate menu cache for restaurant {}: {}", restaurantId, e.getMessage());
        }
    }

    private MenuItemDto toDto(MenuItem item) {
        return MenuItemDto.builder()
                .id(item.getId())
                .restaurantId(item.getRestaurantId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .category(item.getCategory())
                .available(item.isAvailable())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
