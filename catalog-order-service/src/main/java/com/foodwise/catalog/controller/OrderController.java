package com.foodwise.catalog.controller;

import com.foodwise.catalog.dto.CreateOrderRequest;
import com.foodwise.catalog.dto.OrderDto;
import com.foodwise.catalog.dto.OrderStatusUpdateDto;
import com.foodwise.catalog.dto.ValidateResponse;
import com.foodwise.catalog.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, caller));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable Long id,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.getById(id, caller));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateDto statusUpdate,
            Authentication authentication) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.updateStatus(id, statusUpdate, caller));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<Page<OrderDto>> getByRestaurant(
            @PathVariable Long restaurantId,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.getOrdersByRestaurant(restaurantId, caller, pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<OrderDto>> getByCustomer(
            @PathVariable Long customerId,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        ValidateResponse caller = (ValidateResponse) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId, caller, pageable));
    }
}
