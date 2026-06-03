package com.foodwise.catalog.controller;

import com.foodwise.catalog.dto.CreateMenuItemRequest;
import com.foodwise.catalog.dto.MenuItemDto;
import com.foodwise.catalog.dto.ValidateResponse;
import com.foodwise.catalog.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /** Public — browse all available menu items */
    @GetMapping("/items")
    public ResponseEntity<Page<MenuItemDto>> getAllItems(
            @RequestParam(required = false) Long restaurantId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (restaurantId != null) {
            return ResponseEntity.ok(menuService.getRestaurantMenu(restaurantId, pageable));
        }
        return ResponseEntity.ok(menuService.getAllAvailableItems(pageable));
    }

    /** Public — get single menu item */
    @GetMapping("/items/{id}")
    public ResponseEntity<MenuItemDto> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getById(id));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemDto> createItem(
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(request, caller));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemDto> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(menuService.update(id, request, caller));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        menuService.delete(id, caller);
        return ResponseEntity.noContent().build();
    }
}
