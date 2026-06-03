package com.foodwise.inventory.repository;

import com.foodwise.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByRestaurantId(Long restaurantId);

    Optional<InventoryItem> findByIdAndRestaurantId(Long id, Long restaurantId);

    Optional<InventoryItem> findByRestaurantIdAndMenuItemId(Long restaurantId, Long menuItemId);

    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.restaurantId = :restaurantId
        AND (i.quantity < i.threshold OR (i.expiryDate IS NOT NULL AND i.expiryDate <= :expiryThreshold))
        """)
    List<InventoryItem> findSurplusItems(
        @Param("restaurantId") Long restaurantId,
        @Param("expiryThreshold") LocalDate expiryThreshold);

    @Query("""
        SELECT i FROM InventoryItem i
        WHERE (i.quantity < i.threshold OR (i.expiryDate IS NOT NULL AND i.expiryDate <= :expiryThreshold))
        AND i.surplus = false
        """)
    List<InventoryItem> findNewlySurplusItems(@Param("expiryThreshold") LocalDate expiryThreshold);
}
